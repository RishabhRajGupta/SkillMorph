import uuid
from app.services.neo4j_service import graph_db
from app.schemas.graph_models import GoalCreate, TaskCreate
from datetime import date, timedelta

def create_goal_in_db(user_id: str, goal_data: GoalCreate):
    """
    Creates the Goal Node and links it to the User.
    """
    goal_id = str(uuid.uuid4())
    query = """
    MERGE (u:User {id: $user_id})
    CREATE (g:Goal {
        id: $goal_id,
        title: $title,
        category: $category,
        created_at: datetime(),
        total_tasks: 0,
        completed_tasks: 0,
        progress_percentage: 0
    })
    MERGE (u)-[:HAS_GOAL]->(g)
    RETURN g.id as id, g.title as title
    """
    with graph_db.get_session() as session:
        result = session.run(query, user_id=user_id, goal_id=goal_id, title=goal_data.title, category=goal_data.category)
        return result.single()

def add_day_to_goal(goal_id: str, day_number: int, topic: str):
    """
    Adds a Day node to a Goal.
    If it's Day 1, it unlocks immediately.
    If it's Day 2+, it links to the previous day and locks.
    """
    # This query is a bit complex (Tier 1 logic)
    # It finds the Goal, creates the Day, and handles the "Chain" of days.
    query = """
    MATCH (g:Goal {id: $goal_id})
    CREATE (d:Day {
        id: randomUUID(),
        day_number: $day_number,
        topic: $topic,
        is_locked: $is_locked
    })
    MERGE (g)-[:HAS_DAY]->(d)
    
    // If there is a previous day, link to it
    WITH d, g
    MATCH (g)-[:HAS_DAY]->(prev_day:Day)
    WHERE prev_day.day_number = d.day_number - 1
    MERGE (prev_day)-[:UNLOCKS]->(d)
    
    RETURN d.id as id
    """
    
    # Logic: Day 1 is never locked. Day 2+ is locked.
    is_locked = (day_number > 1)
    
    with graph_db.get_session() as session:
        session.run(query, goal_id=goal_id, day_number=day_number, topic=topic, is_locked=is_locked)
        return {"status": "Day Added", "day": day_number}
    

def generate_timeline(goal_id: str, start_date: date, duration_days: int):
    """
    Generates the timeline using Python to calculate specific dates.
    This ensures Day 1 = Today, Day 2 = Tomorrow, etc.
    """
    # 1. Generate the Python List with Dates
    days_data = []
    current_date = start_date
    
    for i in range(1, duration_days + 1):
        days_data.append({
            "day_number": i,
            "date": current_date.isoformat(), # This saves "2024-01-19"
            "topic": f"Day {i}: Pending Generation", 
            "is_locked": i > 1 
        })
        current_date += timedelta(days=1)

    # 2. The Query to Save to Neo4j
    query = """
    MATCH (g:Goal {id: $goal_id})
    
    UNWIND $days_data as day_item
    
    CREATE (d:Day {
        day_number: day_item.day_number,
        topic: day_item.topic,
        // Save the planned date so it shows on the calendar!
        scheduled_date: day_item.date, 
        is_locked: day_item.is_locked,
        is_completed: false,
        sub_tasks: []
    })
    
    MERGE (g)-[:HAS_DAY]->(d)
    
    WITH d
    ORDER BY d.day_number
    WITH collect(d) as days
    
    // Link Day 1 -> Day 2 -> Day 3...
    FOREACH (i in range(0, size(days)-2) |
        FOREACH (d1 in [days[i]] |
            FOREACH (d2 in [days[i+1]] |
                MERGE (d1)-[:UNLOCKS]->(d2)
            ))
    )
    RETURN size(days) as days_created
    """
    
    with graph_db.get_session() as session:
        result = session.run(query, goal_id=goal_id, days_data=days_data)
        record = result.single()
        if record:
            print(f"DEBUG: Success! Created {record['days_created']} day nodes.")
            return record["days_created"]
        return 0
        

def get_goal_roadmap(goal_id: str):
    """
    Fetches the Goal metadata + All Day nodes sorted by day_number.
    """
    query = """
    MATCH (g:Goal {id: $goal_id})
    OPTIONAL MATCH (g)-[:HAS_DAY]->(d:Day)
    RETURN 
        g.title as title, 
        g.category as category,
        collect({
            day_number: d.day_number,
            topic: d.topic,
            is_locked: d.is_locked,
            is_completed: coalesce(d.is_completed, false)
        }) as days
    """
    with graph_db.get_session() as session:
        result = session.run(query, goal_id=goal_id)
        record = result.single()
        
        if not record:
            return None
            
        # Neo4j 'collect' does not guarantee order, so we sort in Python
        data = record.data()
        # Sort days 1..30
        data["days"] = sorted(data["days"], key=lambda x: x["day_number"])
        return data

def get_all_goals(user_id: str):
    """
    Fetches a list of all goals for the user (for the Goals Screen).
    """
    query = """
    MATCH (u:User {id: $user_id})-[:HAS_GOAL]->(g:Goal)
    RETURN g.id as id, g.title as title, g.category as category
    """
    with graph_db.get_session() as session:
        result = session.run(query, user_id=user_id)
        return [record.data() for record in result]

def mark_day_complete(goal_id: str, day_number: int):
    """
    Marks a day as complete, saves the DATE it was finished, and unlocks the next day.
    """
    query = """
    MATCH (g:Goal {id: $goal_id})-[:HAS_DAY]->(current:Day {day_number: $day_number})
    
    // 📅 DATE FIX: Save the completion date (YYYY-MM-DD format)
    SET current.is_completed = true, 
        current.completed_date = toString(date())
    
    WITH g, current
    
    // Unlock next day
    OPTIONAL MATCH (current)-[:UNLOCKS]->(next_day:Day)
    SET next_day.is_locked = false
    
    // Recalculate Progress
    WITH g
    MATCH (g)-[:HAS_DAY]->(d:Day)
    WITH g, count(d) as total_days, sum(CASE WHEN d.is_completed THEN 1 ELSE 0 END) as completed_days
    
    SET g.completed_tasks = completed_days, 
        g.progress_percentage = toInteger((completed_days / toFloat(total_days)) * 100)
        
    RETURN g.progress_percentage as progress
    """
    with graph_db.get_session() as session:
        result = session.run(query, goal_id=goal_id, day_number=day_number)
        record = result.single()
        return record["progress"] if record else 0
    

def update_day_content(goal_id: str, day_number: int, topic: str, sub_tasks: list):
    """
    Overwrites the "Pending Generation" placeholder with real AI content.
    """
    query = """
    MATCH (g:Goal {id: $goal_id})-[:HAS_DAY]->(d:Day {day_number: $day_number})
    SET d.topic = $topic, 
        d.sub_tasks = $sub_tasks
    RETURN d.topic
    """
    with graph_db.get_session() as session:
        session.run(query, goal_id=goal_id, day_number=day_number, topic=topic, sub_tasks=sub_tasks)
        print(f"✅ Updated Day {day_number} with: {topic}")

def get_tasks_for_date(user_id: str, target_date: str):
    """
    Fetches tasks for a specific date (YYYY-MM-DD).
    - Side Quests: Must match 'scheduled_date'.
    - Goal Tasks: 
        1. If COMPLETED: Must have been completed ON this date.
        2. If ACTIVE: Must be the current step AND we are viewing Today.
    """
    query = """
    MATCH (u:User {id: $user_id})
    
    // 1. SIDE QUESTS (Strict Date Match)
    OPTIONAL MATCH (u)-[:HAS_TASK]->(t:Task)
    WHERE t.scheduled_date = $target_date
    
    WITH u, collect(CASE WHEN t IS NULL THEN null ELSE {
        id: t.id,
        title: t.title,
        type: "SIDE_QUEST",
        goal_title: "Side Quest",
        is_completed: coalesce(t.is_completed, false)
    } END) as side_quests
    
    // 2. GOAL TASKS (Smart Logic)
    OPTIONAL MATCH (u)-[:HAS_GOAL]->(g:Goal)-[:HAS_DAY]->(d:Day)
    WHERE 
       // A. It was finished ON this specific day in the past
       (d.completed_date = $target_date)
       OR
       // B. It is UNFINISHED, ACTIVE, and the user is looking at TODAY
       ($target_date = toString(date()) 
        AND coalesce(d.is_completed, false) = false 
        AND coalesce(d.is_locked, false) = false)

    WITH side_quests, collect(CASE WHEN d IS NULL THEN null ELSE {
        id: toString(d.day_number), 
        title: d.topic,
        type: "GOAL",
        goal_title: g.title,
        goal_id: g.id,
        is_completed: coalesce(d.is_completed, false)
    } END) as goal_tasks
    
    RETURN side_quests + goal_tasks as all_tasks
    """
    with graph_db.get_session() as session:
        result = session.run(query, user_id=user_id, target_date=target_date)
        record = result.single()
        if not record: return []
        return record["all_tasks"]
        
def create_side_quest(user_id: str, title: str, scheduled_date: str):
    """
    Creates a Side Quest linked to a specific date.
    """
    query = """
    MATCH (u:User {id: $user_id})
    CREATE (t:Task {
        id: randomUUID(),
        title: $title,
        is_completed: false,
        scheduled_date: $date, 
        created_at: datetime()
    })
    MERGE (u)-[:HAS_TASK]->(t)
    RETURN t.id
    """
    with graph_db.get_session() as session:
        session.run(query, user_id=user_id, title=title, date=scheduled_date)
        return True