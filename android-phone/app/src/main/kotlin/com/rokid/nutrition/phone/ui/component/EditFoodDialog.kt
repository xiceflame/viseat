package com.rokid.nutrition.phone.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.rokid.nutrition.phone.ui.viewmodel.EditableFoodItem
import com.rokid.nutrition.phone.util.FoodItemUpdates
import com.rokid.nutrition.phone.util.NutritionCalculator
import com.rokid.nutrition.phone.util.NutritionValues

/**
 * 食物数据编辑对话框
 * 
 * 支持：
 * 1. 修改食品名称（输入后可查询营养信息）
 * 2. 修改重量（按比例自动计算营养值）
 * 3. 手动修改各项营养值
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditFoodDialog(
    food: EditableFoodItem,
    onSave: (FoodItemUpdates) -> Unit,
    onCancel: () -> Unit,
    onDelete: (() -> Unit)? = null,  // 删除食物回调
    onLookupNutrition: ((String) -> Unit)? = null,  // 查询营养信息回调
    isLoading: Boolean = false,
    validationErrors: Map<String, String> = emptyMap(),
    modifier: Modifier = Modifier
) {
    // 编辑状态
    var foodName by remember { mutableStateOf(food.chineseName ?: food.name) }
    var weightG by remember { mutableStateOf(food.weightG.toString()) }
    var caloriesKcal by remember { mutableStateOf(food.caloriesKcal.toString()) }
    var proteinG by remember { mutableStateOf(food.proteinG.toString()) }
    var carbsG by remember { mutableStateOf(food.carbsG.toString()) }
    var fatG by remember { mutableStateOf(food.fatG.toString()) }
    
    // 是否按比例重新计算
    var recalculateFromWeight by remember { mutableStateOf(true) }
    // 是否显示高级选项
    var showAdvancedOptions by remember { mutableStateOf(false) }

    // 当重量改变时自动计算营养值
    LaunchedEffect(weightG, recalculateFromWeight) {
        if (recalculateFromWeight) {
            val newWeight = weightG.toDoubleOrNull() ?: return@LaunchedEffect
            if (newWeight <= 0) return@LaunchedEffect
            
            val originalNutrition = NutritionValues(
                calories = food.caloriesKcal,
                protein = food.proteinG,
                carbs = food.carbsG,
                fat = food.fatG
            )
            
            val newNutrition = NutritionCalculator.recalculateProportionally(
                original = originalNutrition,
                originalWeight = food.originalWeightG,
                newWeight = newWeight
            )
            
            caloriesKcal = String.format("%.1f", newNutrition.calories)
            proteinG = String.format("%.1f", newNutrition.protein)
            carbsG = String.format("%.1f", newNutrition.carbs)
            fatG = String.format("%.1f", newNutrition.fat)
        }
    }
    
    Dialog(onDismissRequest = { if (!isLoading) onCancel() }) {
        Card(
            modifier = modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // 标题
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "编辑食物数据",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
                
                if (food.isEdited) {
                    Text(
                        text = "已编辑",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 食品名称输入（主要编辑项）
                OutlinedTextField(
                    value = foodName,
                    onValueChange = { foodName = it },
                    label = { Text("食品名称") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Restaurant,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        if (onLookupNutrition != null && foodName.isNotBlank()) {
                            IconButton(
                                onClick = { onLookupNutrition(foodName) },
                                enabled = !isLoading
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "查询营养信息",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    },
                    singleLine = true,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    isError = validationErrors.containsKey("foodName")
                )
                if (validationErrors.containsKey("foodName")) {
                    Text(
                        text = validationErrors["foodName"] ?: "",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // 重量输入（主要编辑项）
                NutritionTextField(
                    value = weightG,
                    onValueChange = { weightG = it },
                    label = "重量",
                    unit = "g",
                    icon = Icons.Default.Scale,
                    error = validationErrors["weightG"],
                    enabled = !isLoading
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 热量显示（大字体突出）
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFF6B6B).copy(alpha = 0.1f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = caloriesKcal.toDoubleOrNull()?.toInt()?.toString() ?: "0",
                            fontWeight = FontWeight.Bold,
                            fontSize = 36.sp,
                            color = Color(0xFFFF6B6B)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "kcal",
                            fontSize = 16.sp,
                            color = Color(0xFFFF6B6B).copy(alpha = 0.7f),
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // 营养素简要显示
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    NutrientChip("蛋白质", "${proteinG.toDoubleOrNull()?.toInt() ?: 0}g", Color(0xFF4ECDC4))
                    NutrientChip("碳水", "${carbsG.toDoubleOrNull()?.toInt() ?: 0}g", Color(0xFFFFE66D))
                    NutrientChip("脂肪", "${fatG.toDoubleOrNull()?.toInt() ?: 0}g", Color(0xFF95E1D3))
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 高级选项折叠
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "高级选项",
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                    IconButton(onClick = { showAdvancedOptions = !showAdvancedOptions }) {
                        Icon(
                            imageVector = if (showAdvancedOptions) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (showAdvancedOptions) "收起" else "展开",
                            tint = Color.Gray
                        )
                    }
                }

                // 高级选项内容
                if (showAdvancedOptions) {
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // 按比例计算开关
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "按比例计算",
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "修改重量时自动计算营养值",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                        Switch(
                            checked = recalculateFromWeight,
                            onCheckedChange = { recalculateFromWeight = it }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // 热量输入
                    NutritionTextField(
                        value = caloriesKcal,
                        onValueChange = { 
                            caloriesKcal = it
                            recalculateFromWeight = false
                        },
                        label = "热量",
                        unit = "kcal",
                        icon = Icons.Default.LocalFireDepartment,
                        error = validationErrors["caloriesKcal"],
                        enabled = !isLoading
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // 蛋白质输入
                    NutritionTextField(
                        value = proteinG,
                        onValueChange = { 
                            proteinG = it
                            recalculateFromWeight = false
                        },
                        label = "蛋白质",
                        unit = "g",
                        icon = Icons.Default.FitnessCenter,
                        error = validationErrors["proteinG"],
                        enabled = !isLoading
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // 碳水输入
                    NutritionTextField(
                        value = carbsG,
                        onValueChange = { 
                            carbsG = it
                            recalculateFromWeight = false
                        },
                        label = "碳水化合物",
                        unit = "g",
                        icon = Icons.Default.Grain,
                        error = validationErrors["carbsG"],
                        enabled = !isLoading
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // 脂肪输入
                    NutritionTextField(
                        value = fatG,
                        onValueChange = { 
                            fatG = it
                            recalculateFromWeight = false
                        },
                        label = "脂肪",
                        unit = "g",
                        icon = Icons.Default.WaterDrop,
                        error = validationErrors["fatG"],
                        enabled = !isLoading
                    )
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // 按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // 删除按钮（左侧）
                    if (onDelete != null) {
                        TextButton(
                            onClick = onDelete,
                            enabled = !isLoading,
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = Color(0xFFE53935)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("删除")
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }
                    
                    // 取消和保存按钮（右侧）
                    Row {
                        TextButton(
                            onClick = onCancel,
                            enabled = !isLoading
                        ) {
                            Text("取消")
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Button(
                            onClick = {
                                val updates = FoodItemUpdates(
                                    foodName = if (foodName != (food.chineseName ?: food.name)) foodName else null,
                                    weightG = weightG.toDoubleOrNull(),
                                    caloriesKcal = caloriesKcal.toDoubleOrNull(),
                                    proteinG = proteinG.toDoubleOrNull(),
                                    carbsG = carbsG.toDoubleOrNull(),
                                    fatG = fatG.toDoubleOrNull(),
                                    recalculateFromWeight = false
                                )
                                onSave(updates)
                            },
                            enabled = !isLoading
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text("保存")
                        }
                    }
                }
            }
        }
    }
}

/**
 * 营养素小标签
 */
@Composable
private fun NutrientChip(label: String, value: String, color: Color) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = color.copy(alpha = 0.15f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = color
            )
            Text(
                text = label,
                fontSize = 11.sp,
                color = color.copy(alpha = 0.8f)
            )
        }
    }
}

/**
 * 营养值输入框
 */
@Composable
private fun NutritionTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    unit: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    error: String? = null,
    enabled: Boolean = true
) {
    Column {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            leadingIcon = {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (error != null) MaterialTheme.colorScheme.error 
                           else MaterialTheme.colorScheme.primary
                )
            },
            trailingIcon = {
                Text(
                    text = unit,
                    color = Color.Gray,
                    modifier = Modifier.padding(end = 8.dp)
                )
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            isError = error != null,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth()
        )
        
        if (error != null) {
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}
