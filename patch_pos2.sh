sed -i '187,211c\
@Composable\
fun ProductCard(product: Product, onClick: () -> Unit, onEdit: () -> Unit) {\
    Card(\
        modifier = Modifier.fillMaxWidth().clickable { onClick() },\
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),\
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)\
    ) {\
        Column(modifier = Modifier.padding(12.dp)) {\
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {\
                Text(product.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))\
                IconButton(onClick = onEdit, modifier = Modifier.size(24.dp)) {\
                    Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))\
                }\
            }\
            Spacer(modifier = Modifier.height(4.dp))\
            Text(product.category, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)\
            if (product.stockCount >= 0) {\
                Text("Stock: ${product.stockCount}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)\
            }\
            Spacer(modifier = Modifier.height(8.dp))\
            Text("Rs. ${"%.2f".format(product.price)}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)\
        }\
    }\
}' app/src/main/java/com/example/ui/pos/PosScreen.kt
