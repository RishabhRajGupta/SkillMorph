from app.services.neo4j_service import graph_db
from app.schemas.graph_models import GoalCreate, TaskCreate
from datetime import date, timedelta

def create_goal_in_db(user_id: str, goal: GoalCreate):
    """
    Creates a Goal node and links it to the User.
    (User)-[:HAS_GOAL]->(Goal)
    """
    query = """
    MERGE (u:User {id: $user_id})
    CREATE (g:Goal {
        id: randomUUID(),
        title: $title,
        category: $category,
        created_at: datetime()
    })
    MERGE (u)-[:HAS_GOAL]->(g)
    RETURN g.id as id, g.title as title
    """
    
    with graph_db.get_session() as session:
        result = session.run(query, user_id=user_id, title=goal.title, category=goal.category)
        record = result.single()
        if record:
            return record.data()
        return None

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
    # DEBUG PRINT 1: Check inputs
    print(f"DEBUG: Generating timeline for Goal ID: '{goal_id}'")
    print(f"DEBUG: Start Date: {start_date}, Duration: {duration_days}")

    # 1. Generate the Python List
    days_data = []
    current_date = start_date
    
    for i in range(1, duration_days + 1):
        days_data.append({
            "day_number": i,
            "date": current_date.isoformat(), 
            "topic": f"Day {i}: Pending Generation", 
            "is_locked": i > 1 
        })
        current_date += timedelta(days=1)
    
    # DEBUG PRINT 2: Check list size
    print(f"DEBUG: Python generated {len(days_data)} day objects.")

    # 2. The Query
    query = """
    MATCH (g:Goal {id: $goal_id})
    
    // Check if we actually found the goal
    WITH g
    
    UNWIND $days_data as day_item
    
    CREATE (d:Day {
        id: randomUUID(),
        day_number: day_item.day_number,
        date: date(day_item.date),
        topic: day_item.topic,
        is_locked: day_item.is_locked
    })
    
    MERGE (g)-[:HAS_DAY]->(d)
    
    WITH d, day_item
    ORDER BY d.day_number
    WITH collect(d) as days
    
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
        
        # DEBUG PRINT 3: Check database result
        if record:
            count = record["days_created"]
            print(f"DEBUG: Success! Neo4j created {count} nodes.")
            return count
        else:
            print("DEBUG: FAILURE! Database returned None. This means the Goal ID was not found.")
            return 0