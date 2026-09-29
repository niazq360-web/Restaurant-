sed -i '188,202c\
@Composable\
fun ProductCard(product: Product, onClick: () -> Unit) {\
    Card(\
        modifier = Modifier.fillMaxWidth().clickable { onClick() },\
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),\
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)\
    ) {\
        Column {\
            val imgUrl = product.imageUrl ?: "https://picsum.photos/seed/${product.name.replace(" ", "")}/200"\
            AsyncImage(\
                model = imgUrl,\
                contentDescription = product.name,\
                contentScale = ContentScale.Crop,\
                modifier = Modifier.fillMaxWidth().height(100.dp).clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))\
            )\
            Column(modifier = Modifier.padding(12.dp)) {\
                Text(product.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)\
                Spacer(modifier = Modifier.height(4.dp))\
                Text(product.category, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)\
                Spacer(modifier = Modifier.height(8.dp))\
                Text("Rs. ${"%.2f".format(product.price)}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)\
            }\
        }\
    }\
}' app/src/main/java/com/example/ui/pos/PosScreen.kt
