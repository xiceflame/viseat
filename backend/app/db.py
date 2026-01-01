import os
import logging
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker, declarative_base

logger = logging.getLogger(__name__)

# 优先级：环境变量 DATABASE_URL > 默认 SQLite
DATABASE_URL = os.getenv("DATABASE_URL", "sqlite:///./app.db")

# 适配 SQLAlchemy 2.0 + psycopg3
if DATABASE_URL.startswith("postgresql://"):
    DATABASE_URL = DATABASE_URL.replace("postgresql://", "postgresql+psycopg://", 1)

# 连接参数配置
connect_args = {}
engine_args = {
    "echo": False,
    "future": True,
}

if DATABASE_URL.startswith("sqlite"):
    connect_args["check_same_thread"] = False
else:
    # Postgres 云端连接优化
    engine_args.update({
        "pool_size": 20,
        "max_overflow": 10,
        "pool_timeout": 30,
        "pool_recycle": 1800,
        "pool_pre_ping": True,  # 每次从池中取连接前先检查是否存活
    })

logger.info(f"正在连接数据库: {DATABASE_URL.split('@')[-1] if '@' in DATABASE_URL else DATABASE_URL}")

try:
    engine = create_engine(DATABASE_URL, connect_args=connect_args, **engine_args)
    SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine, future=True)
except Exception as e:
    logger.error(f"创建数据库引擎失败: {e}")
    raise

Base = declarative_base()

def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()

def init_db():
    from . import models
    from . import models_extended
    try:
        Base.metadata.create_all(bind=engine)
        logger.info("数据库初始化完成")
    except Exception as e:
        logger.error(f"数据库初始化失败: {e}")
