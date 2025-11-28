package com.rokid.nutrition.phone.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rokid.nutrition.phone.bluetooth.ConnectionState
import com.rokid.nutrition.phone.bluetooth.GlassInfo

/**
 * 设备管理页面
 */
@Composable
fun DeviceScreen(
    connectionState: ConnectionState,
    glassInfo: GlassInfo?,
    onConnectClick: () -> Unit,
    onDisconnectClick: () -> Unit,
    onForgetDeviceClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "设备管理",
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // 设备信息卡片
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 状态指示灯
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(
                                when (connectionState) {
                                    is ConnectionState.Connected -> Color(0xFF4CAF50)
                                    is ConnectionState.Connecting -> Color(0xFFFFC107)
                                    else -> Color(0xFF9E9E9E)
                                }
                            )
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Text(
                        text = when (connectionState) {
                            is ConnectionState.Connected -> "已连接"
                            is ConnectionState.Connecting -> "正在连接..."
                            is ConnectionState.Error -> "连接失败"
                            else -> "未连接"
                        },
                        fontWeight = FontWeight.Medium,
                        fontSize = 18.sp
                    )
                }
                
                if (connectionState is ConnectionState.Connected && glassInfo != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    DeviceInfoRow("设备名称", glassInfo.name)
                    DeviceInfoRow("固件版本", glassInfo.firmwareVersion)
                    DeviceInfoRow("序列号", glassInfo.serialNumber)
                    
                    if (glassInfo.batteryLevel > 0) {
                        DeviceInfoRow(
                            "电量",
                            "${glassInfo.batteryLevel}%" + if (glassInfo.isCharging) " (充电中)" else ""
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // 操作按钮
        when (connectionState) {
            is ConnectionState.Connected -> {
                OutlinedButton(
                    onClick = onDisconnectClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFFF5722)
                    )
                ) {
                    Text("断开连接")
                }
            }
            is ConnectionState.Connecting -> {
                Button(
                    onClick = {},
                    enabled = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("连接中...")
                }
            }
            else -> {
                Button(
                    onClick = onConnectClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("连接眼镜")
                }
                
                // 显示错误信息和忘记设备按钮
                if (connectionState is ConnectionState.Error) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = (connectionState as ConnectionState.Error).message,
                        color = Color(0xFFFF5722),
                        fontSize = 14.sp
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedButton(
                        onClick = onForgetDeviceClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF9E9E9E)
                        )
                    ) {
                        Text("忘记设备并重新配对")
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Color.Gray, fontSize = 14.sp)
        Text(text = value, fontSize = 14.sp)
    }
}
