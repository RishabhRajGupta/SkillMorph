# SkillMorph

## Project Architecture

### Frontend (Android)

```
app/
├── java/
│   └── com/
│       └── example/
│           └── skillmorph/
│               ├── di/
│               │   ├── AuthInterceptor.kt
│               │   ├── AuthModule.kt
│               │   ├── DatabaseModule.kt
│               │   ├── NetworkModule.kt
│               │   └── SharedPreferences.kt
│               ├── ui/
│               │   └── theme/
│               │       ├── Color.kt
│               │       ├── Theme.kt
│               │       └── Type.kt
│               ├── data/
│               │   ├── local/
│               │   │   └── entities/
│               │   │       ├── ChatDao.kt
│               │   │       ├── ChatEntity.kt
│               │   │       ├── ChatMessageEntity.kt
│               │   │       ├── GoalEntity.kt
│               │   │       └── TaskEntity.kt
│               │   ├── remote/
│               │   │   └── SkillMorphApi.kt
│               │   └── repository/
│               │       └── AuthRepositoryImpl.kt
│               ├── domain/
│               │   └── repository/
│               │       ├── AuthRepository.kt
│               │       └── TasksRepository.kt
│               ├── presentation/
│               │   ├── auth/
│               │   │   ├── AuthScreen.kt
│               │   │   └── AuthViewModel.kt
│               │   ├── main/
│               │   │   ├── viewModel/
│               │   │   ├── DailyBrieferWorker.kt
│               │   │   └── MainScreen.kt
│               │   ├── goals/
│               │   │   ├── components/
│               │   │   ├── GoalsScreen.kt
│               │   │   └── GoalsViewModel.kt
│               │   ├── tasks/
│               │   │   ├── TasksScreen.kt
│               │   │   └── TasksViewModel.kt
│               │   ├── Profile/
│               │   │   ├── ProfileScreen.kt
│               │   │   └── ProfileViewModel.kt
│               │   ├── goaldetail/
│               │   │   ├── models/
│               │   │   ├── MetroMapScreen.kt
│               │   │   └── MetroMapViewModel.kt
│               │   └── navigation/
│               │       ├── AppNavigation.kt
│               │       └── Screen.kt
│               ├── HomeScreen.kt
│               ├── MainActivity.kt
│               └── SkillMorphApp.kt
└── res/
```

### Backend (Python)

```
.vscode/
app/
├── __pycache__/
├── agent/
│   ├── __pycache__/
│   ├── graph.py
│   ├── nodes.py
│   ├── state.py
│   └── tools.py
├── core/
│   ├── __pycache__/
│   ├── __init__.py
│   ├── config.py
│   └── gemini_key.py
├── schemas/
│   ├── __pycache__/
│   └── graph_models.py
├── services/
│   ├── __pycache__/
│   ├── __init__.py
│   ├── chat_session_service.py
│   ├── graph_crud.py
│   ├── llm_service.py
│   ├── memory_service.py
│   └── neo4j_service.py
├── __init__.py
└── main.py
.env
.gitignore
```
