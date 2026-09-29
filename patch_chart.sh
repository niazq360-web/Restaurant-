sed -i '177,189c\
            val labelLayout = textMeasurer.measure(text = pair.first, style = textStyle)\
            drawText(\
                textLayoutResult = labelLayout,\
                topLeft = Offset(x - 20.dp.toPx(), size.height + 8.dp.toPx())\
            )\
            \
            val valueLayout = textMeasurer.measure(text = pair.second.toInt().toString(), style = textStyle)\
            drawText(\
                textLayoutResult = valueLayout,\
                topLeft = Offset(x - 10.dp.toPx(), y - 24.dp.toPx())\
            )\
' app/src/main/java/com/example/ui/summary/SalesSummaryScreen.kt
