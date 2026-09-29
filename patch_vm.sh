sed -i '16,19c\
    val orders: StateFlow<List<Order>> = repository.allOrders\
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())\
    val allOrderItems: StateFlow<List<OrderItem>> = repository.allOrderItems\
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())\
' app/src/main/java/com/example/ui/pos/PosViewModel.kt
