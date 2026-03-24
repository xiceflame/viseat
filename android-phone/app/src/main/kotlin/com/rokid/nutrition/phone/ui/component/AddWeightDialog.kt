@file:OptIn(ExperimentalMaterial3Api::class)

package com.rokid.nutrition.phone.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.rokid.nutrition.phone.ui.theme.*

/**
 * 添加体重记录对话框
 */
@Composable
fun AddWeightDialog(
    currentWeight: Float?,
    onDismiss: () -> Unit,
    onConfirm: (weight: Float, note: String?) -> Unit
) {
    var weightText by remember { mutableStateOf(currentWeight?.toString() ?: "") }
    var note by remember { mutableStateOf("") }
    var isWeightValid by remember { mutableStateOf(currentWeight != null) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // 标题
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "记录体重",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = "关闭",
                            tint = AppleGray2
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // 体重输入
                OutlinedTextField(
                    value = weightText,
                    onValueChange = { newValue ->
                        // 只允许数字和小数点
                        val filtered = newValue.filter { it.isDigit() || it == '.' }
                        // 限制只有一个小数点
                        val dotCount = filtered.count { it == '.' }
                        if (dotCount <= 1) {
                            weightText = filtered
                            isWeightValid = filtered.toFloatOrNull()?.let { it > 0 && it < 500 } == true
                        }
                    },
                    label = { Text("体重 (kg)") },
                    placeholder = { Text("例如: 65.5") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = weightText.isNotEmpty() && !isWeightValid,
                    supportingText = if (weightText.isNotEmpty() && !isWeightValid) {
                        { Text("请输入有效的体重 (1-500 kg)", color = MaterialTheme.colorScheme.error) }
                    } else null,
                    leadingIcon = {
                        Icon(
                            Icons.Rounded.MonitorWeight,
                            contentDescription = null,
                            tint = AppleBlue
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 备注输入
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("备注 (可选)") },
                    placeholder = { Text("例如: 早餐前测量") },
                    leadingIcon = {
                        Icon(
                            Icons.Rounded.Notes,
                            contentDescription = null,
                            tint = AppleGray2
                        )
                    },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // 按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("取消")
                    }
                    
                    Button(
                        onClick = {
                            weightText.toFloatOrNull()?.let { weight ->
                                onConfirm(weight, note.takeIf { it.isNotBlank() })
                            }
                        },
                        enabled = isWeightValid,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = AppleBlue),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("保存")
                    }
                }
            }
        }
    }
}

/**
 * 编辑体重记录对话框
 */
@Composable
fun EditWeightDialog(
    weight: Float,
    note: String?,
    onDismiss: () -> Unit,
    onConfirm: (weight: Float, note: String?) -> Unit,
    onDelete: () -> Unit
) {
    var weightText by remember { mutableStateOf(weight.toString()) }
    var noteText by remember { mutableStateOf(note ?: "") }
    var isWeightValid by remember { mutableStateOf(true) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            icon = { Icon(Icons.Rounded.Delete, contentDescription = null, tint = AppleRed) },
            title = { Text("删除记录？") },
            text = { Text("确定要删除这条体重记录吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = AppleRed)
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("取消")
                }
            }
        )
    } else {
        Dialog(onDismissRequest = onDismiss) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    // 标题
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "编辑记录",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Row {
                            IconButton(
                                onClick = { showDeleteConfirm = true },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.Delete,
                                    contentDescription = "删除",
                                    tint = AppleRed
                                )
                            }
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.Close,
                                    contentDescription = "关闭",
                                    tint = AppleGray2
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // 体重输入
                    OutlinedTextField(
                        value = weightText,
                        onValueChange = { newValue ->
                            val filtered = newValue.filter { it.isDigit() || it == '.' }
                            val dotCount = filtered.count { it == '.' }
                            if (dotCount <= 1) {
                                weightText = filtered
                                isWeightValid = filtered.toFloatOrNull()?.let { it > 0 && it < 500 } == true
                            }
                        },
                        label = { Text("体重 (kg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = weightText.isNotEmpty() && !isWeightValid,
                        leadingIcon = {
                            Icon(
                                Icons.Rounded.MonitorWeight,
                                contentDescription = null,
                                tint = AppleBlue
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // 备注输入
                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        label = { Text("备注 (可选)") },
                        leadingIcon = {
                            Icon(
                                Icons.Rounded.Notes,
                                contentDescription = null,
                                tint = AppleGray2
                            )
                        },
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // 按钮
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("取消")
                        }
                        
                        Button(
                            onClick = {
                                weightText.toFloatOrNull()?.let { w ->
                                    onConfirm(w, noteText.takeIf { it.isNotBlank() })
                                }
                            },
                            enabled = isWeightValid,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = AppleBlue),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("保存")
                        }
                    }
                }
            }
        }
    }
}
