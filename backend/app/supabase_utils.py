import os
from typing import Optional
from jose import jwt, JWTError
from fastapi import HTTPException, Security
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from supabase import create_client, Client

# Supabase 配置
SUPABASE_URL = os.getenv("SUPABASE_URL")
SUPABASE_ANON_KEY = os.getenv("SUPABASE_ANON_KEY")
# Supabase JWT 密钥通常在项目设置 -> API -> JWT Secret 中获取
# 这里建议用户将其添加到 .env
SUPABASE_JWT_SECRET = os.getenv("SUPABASE_JWT_SECRET")

# 初始化 Supabase 客户端
supabase: Client = create_client(SUPABASE_URL, SUPABASE_ANON_KEY) if SUPABASE_URL and SUPABASE_ANON_KEY else None

security = HTTPBearer()

async def get_current_user(auth: HTTPAuthorizationCredentials = Security(security)):
    """
    验证 Supabase JWT Token 并返回用户信息
    """
    if not SUPABASE_JWT_SECRET:
        raise HTTPException(
            status_code=500,
            detail="SUPABASE_JWT_SECRET not configured in backend"
        )
    
    token = auth.credentials
    try:
        # 验证 JWT 签名
        payload = jwt.decode(
            token, 
            SUPABASE_JWT_SECRET, 
            algorithms=["HS256"], 
            audience="authenticated"
        )
        user_id: str = payload.get("sub")
        if user_id is None:
            raise HTTPException(status_code=401, detail="Invalid token: missing sub")
        return {"user_id": user_id, "email": payload.get("email")}
    except JWTError as e:
        raise HTTPException(status_code=401, detail=f"Could not validate credentials: {str(e)}")

async def upload_image_to_supabase(file_path: str, bucket_name: str = "meal-images") -> str:
    """
    将本地图片上传到 Supabase Storage 并返回公开访问 URL
    """
    if not supabase:
        raise RuntimeError("Supabase client not initialized")
    
    file_name = os.path.basename(file_path)
    with open(file_path, "rb") as f:
        # 上传文件
        res = supabase.storage.from_(bucket_name).upload(
            path=file_name,
            file=f,
            file_options={"content-type": "image/jpeg"}
        )
    
    # 获取公开 URL
    url = supabase.storage.from_(bucket_name).get_public_url(file_name)
    return url
