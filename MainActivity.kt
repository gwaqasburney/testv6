package com.example.android.wearable.composeforwearos


import MyDatabaseHelper
import android.content.ContentValues
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.foundation.lazy.AutoCenteringParams
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.Vignette
import androidx.wear.compose.material.VignettePosition
import androidx.wear.compose.material.scrollAway
import com.example.android.wearable.composeforwearos.theme.WearAppTheme
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone




val handler = Handler(Looper.getMainLooper())
var runnable: Runnable? = null
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WearApp(this.applicationContext)
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}


@Composable
fun WearApp(context: Context) {
    WearAppTheme {
        // TODO: Swap to ScalingLazyListState
        val listState = rememberScalingLazyListState()


        /* *************************** Part 4: Wear OS Scaffold *************************** */
        // TODO (Start): Create a Scaffold (Wear Version)
        Scaffold(
            timeText = {
                TimeText(modifier = Modifier.scrollAway(listState))
            },
            vignette = {
                // Only show a Vignette for scrollable screens. This code lab only has one screen,
                // which is scrollable, so we show it all the time.
                Vignette(vignettePosition = VignettePosition.TopAndBottom)
            },
            positionIndicator = {
                PositionIndicator(
                    scalingLazyListState = listState
                )
            }
        ) {


            // Modifiers used by our Wear composable.
            val contentModifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
            val iconModifier = Modifier
                .size(24.dp)
                .wrapContentSize(align = Alignment.Center)


            /* *************************** Part 3: ScalingLazyColumn *************************** */
            // TODO: Swap a ScalingLazyColumn (Wear's version of LazyColumn)
            ScalingLazyColumn(
                modifier = Modifier.fillMaxSize(),
                autoCentering = AutoCenteringParams(itemIndex = 0),
                state = listState
            ) {


                /* ******************* Part 1: Simple composable ******************* */
                //item { ButtonExample(contentModifier, iconModifier) }
                //item { TextExample(contentModifier) }
                item { CardExample(contentModifier, iconModifier, context)
                }
                //item { CardExample(contentModifier, iconModifier) }


                /* ********************* Part 2: Wear unique composable ********************* */
                //item { ChipExample(contentModifier, iconModifier) }
                //item { ToggleChipExample(contentModifier) }
            }


            // TODO (End): Create a Scaffold (Wear Version)
        }
    }
}


class RecordViewModel : ViewModel() {
    val firstRecord = MutableLiveData("")
    val lastRecord = MutableLiveData("")
    val idealRecords = MutableLiveData("")
    val capturedRecords = MutableLiveData("")
    var isRunnableRunning = false
    var isFirstRun = true
}




@Composable
fun displayRecords(context: Context) {
    val viewModel: RecordViewModel = viewModel()
    val firstRecord by viewModel.firstRecord.observeAsState("Initial Value")
    val lastRecord by viewModel.lastRecord.observeAsState("Initial Value")
    val idealRecords by viewModel.idealRecords.observeAsState("Initial Value")
    val capturedRecords by viewModel.capturedRecords.observeAsState("Initial Value")
    startStoringTimestamps(context, viewModel)


    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left-aligned text
        Column(
            horizontalAlignment = Alignment.Start, // Left align
            verticalArrangement = Arrangement.Top
        ) {
            val textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
            Text("First: $firstRecord", style = textStyle)
            Text("Last: $lastRecord", style = textStyle)
            Text("Ideal: $idealRecords", style = textStyle)
            Text("Real: $capturedRecords", style = textStyle)
        }


        // Right-aligned buttons
        Column(
            horizontalAlignment = Alignment.End, // Right align
            verticalArrangement = Arrangement.Top,
            modifier = Modifier.padding(start = 16.dp) // optional padding
        ) {
            DeleteForeverButton(context, viewModel)
            Spacer(modifier = Modifier.height(0.dp)) // Add space between buttons
            KillButton()


        }
    }
}


fun startStoringTimestamps(context: Context, viewModel: RecordViewModel) {
    if (viewModel.isRunnableRunning) return
    viewModel.isRunnableRunning = true
    viewModel.isFirstRun = true


    val initialDelay = 0L


    runnable = object : Runnable {
        override fun run() {
            val timestamp = Timestamp(Date().time)
            saveTimestamp(timestamp, context, viewModel, viewModel.isFirstRun)
            viewModel.isFirstRun = false
            handler.postDelayed(this, 1000)
        }
    }
    handler.postDelayed(runnable!!, initialDelay)
}




fun saveTimestamp(timestamp: Timestamp, context: Context, viewModel: RecordViewModel, isFirstRun: Boolean) {
    val dbHelper = MyDatabaseHelper.getInstance(context)
    val simpleDateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).apply {
        timeZone = TimeZone.getDefault()
    }


// Insert timestamp into database
    dbHelper.writableDatabase.use { db ->
        db.insert("timestamps", null, ContentValues().apply {
            put("timestamp", timestamp.time)
        })
    }


// Update ViewModel
    dbHelper.let {
        // Update the captured records count, but only if this is not the first run.
        if (!isFirstRun) {
            viewModel.capturedRecords.value = it.getTotalRecords().toString()
        }
        viewModel.isFirstRun = false
        // Get the first record from the database.
        it.getFirstRecord()?.let { firstRecord ->
            // Update the first record and ideal records fields in the ViewModel.
            viewModel.firstRecord.value = simpleDateFormat.format(firstRecord)
            viewModel.idealRecords.value = ((Date().time - firstRecord.time) / 1000).toString()
        } ?: run {
            // If there is no first record, set the first record and ideal records fields in the ViewModel to empty strings.
            viewModel.firstRecord.value = ""
            viewModel.idealRecords.value = ""
        }


        // Update the last record field in the ViewModel.
        viewModel.lastRecord.value = simpleDateFormat.format(timestamp)
    }
}


@Composable
fun KillButton() {
    Button(
        onClick = {
            android.os.Process.killProcess(android.os.Process.myPid())
        },
        modifier = Modifier.size(40.dp), // Adjust this for the desired button size
        colors = ButtonDefaults.buttonColors(backgroundColor = Color.Transparent, contentColor = Color.Red)
    ) {
        Icon(Icons.Default.Cancel, contentDescription = "Cancel", modifier = Modifier.size(24.dp))
    }
}


@Composable
fun DeleteForeverButton(context: Context, viewModel: RecordViewModel) {
    Button(
        onClick = {
            MyDatabaseHelper.getInstance(context).clearDatabase()
            handler.removeCallbacks(runnable!!) // Remove the Runnable
            viewModel.isRunnableRunning = false
            viewModel.isFirstRun = true
            viewModel.firstRecord.value = ""
            viewModel.lastRecord.value = ""
            viewModel.idealRecords.value = ""
            viewModel.capturedRecords.value = ""
        },
        modifier = Modifier.size(40.dp),
        colors = ButtonDefaults.buttonColors(backgroundColor = Color.Transparent, contentColor = Color.LightGray)
    ) {
        Icon(Icons.Default.DeleteForever, contentDescription = "Delete", modifier = Modifier.size(24.dp))
    }
}
