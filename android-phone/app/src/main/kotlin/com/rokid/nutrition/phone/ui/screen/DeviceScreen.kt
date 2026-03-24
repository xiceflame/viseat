package com.rokid.nutrition.phone.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rokid.nutrition.phone.bluetooth.ConnectionState
import com.rokid.nutrition.phone.bluetooth.GlassInfo
import com.rokid.nutrition.phone.ui.theme.*

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
            .background(PremiumGradients.pageBackground())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(
            text = "设备管理",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // 设备信息卡片
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 4.dp,
                    shape = RoundedCornerShape(20.dp),
                    ambientColor = Color.Black.copy(alpha = 0.05f),
                    spotColor = Color.Black.copy(alpha = 0.1f)
                ),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 状态图标
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                when (connectionState) {
                                    is ConnectionState.Connected -> AppleTeal.copy(alpha = 0.12f)
                                    is ConnectionState.Connecting -> AppleOrange.copy(alpha = 0.12f)
                                    else -> AppleGray5
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            when (connectionState) {
                                is ConnectionState.Connected -> Icons.Rounded.BluetoothConnected
                                is ConnectionState.Connecting -> Icons.Rounded.BluetoothSearching
                                else -> Icons.Rounded.BluetoothDisabled
                            },
                            contentDescription = null,
                            tint = when (connectionState) {
                                is ConnectionState.Connected -> AppleTeal
                                is ConnectionState.Connecting -> AppleOrange
                                else -> AppleGray2
                            },
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column {
                        Text(
                            text = when (connectionState) {
                                is ConnectionState.Connected -> "已连接"
                                is ConnectionState.Connecting -> "正在连接..."
                                is ConnectionState.Error -> "连接失败"
                                else -> "未连接"
                            },
                            style = MaterialTheme.typography.headlineMedium,
                            color = when (connectionState) {
                                is ConnectionState.Connected -> AppleTeal
                                is ConnectionState.Connecting -> AppleOrange
                                is ConnectionState.Error -> AppleRed
                                else -> AppleGray1
                            }
                        )
                        Text(
                            text = when (connectionState) {
                                is ConnectionState.Connected -> "Rokid 智能眼镜"
                                is ConnectionState.Connecting -> "正在搜索设备..."
                                else -> "点击下方按钮连接"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = AppleGray1
                        )
                    }
                }
                
                if (connectionState is ConnectionState.Connected && glassInfo != null) {
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = AppleGray6
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            DeviceInfoRow("设备名称", glassInfo.name)
                            DeviceInfoRow("固件版本", glassInfo.firmwareVersion)
                            DeviceInfoRow("序列号", glassInfo.serialNumber)
                            
                            if (glassInfo.batteryLevel > 0) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "电量",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = AppleGray1
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            if (glassInfo.isCharging) Icons.Rounded.BatteryChargingFull else Icons.Rounded.Battery5Bar,
                                            contentDescription = null,
                                            tint = if (glassInfo.batteryLevel > 20) AppleTeal else AppleRed,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "${glassInfo.batteryLevel}%" + if (glassInfo.isCharging) " 充电中" else "",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = if (glassInfo.batteryLevel > 20) AppleTeal else AppleRed
                                        )
                                    }
                                }
                            }
                        }
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
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = ApplePink
                    )
                ) {
                    Icon(
                        Icons.Rounded.LinkOff,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("断开连接", style = MaterialTheme.typography.titleMedium)
                }
            }
            is ConnectionState.Connecting -> {
                Button(
                    onClick = {},
                    enabled = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppleTeal,
                        disabledContainerColor = AppleTeal.copy(alpha = 0.5f)
                    )
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("连接中...", style = MaterialTheme.typography.titleMedium)
                }
            }
            else -> {
                Button(
                    onClick = onConnectClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AppleTeal)
                ) {
                    Icon(
                        Icons.Rounded.Bluetooth,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("连接眼镜", style = MaterialTheme.typography.titleMedium)
                }
                
                // 显示错误信息和忘记设备按钮
                if (connectionState is ConnectionState.Error) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = AppleRed.copy(alpha = 0.1f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Rounded.ErrorOutline,
                                contentDescription = null,
                                tint = AppleRed,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = (connectionState as ConnectionState.Error).message,
                                style = MaterialTheme.typography.bodySmall,
                                color = AppleRed
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedButton(
                        onClick = onForgetDeviceClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = AppleGray1
                        )
                    ) {
                        Icon(
                            Icons.Rounded.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("忘记设备并重新配对", style = MaterialTheme.typography.bodyMedium)
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
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = AppleGray1
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
