class LiveGraphActivity : AppCompatActivity() {
    
    private var isCartesianMode = true
    private lateinit var coordinateSystemReceiver: BroadcastReceiver
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Get coordinate system from intent
        val coordinateSystem = intent.getStringExtra("coordinate_system")
        isCartesianMode = coordinateSystem != "polar"
        
        // Register broadcast receiver for coordinate system changes
        registerCoordinateSystemReceiver()
        
        // Setup graphs with current coordinate system
        updateGraphLabels()
    }
    
    private fun registerCoordinateSystemReceiver() {
        coordinateSystemReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                isCartesianMode = intent.getBooleanExtra("is_cartesian", true)
                updateGraphLabels()
            }
        }
        
        val filter = IntentFilter("COORDINATE_SYSTEM_CHANGED")
        registerReceiver(coordinateSystemReceiver, filter)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(coordinateSystemReceiver)
    }
}
