package io.github.takusan23.mobiledatausage

import android.app.AppOpsManager
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.widget.TextView
import java.util.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // findViewByIdよりViewBindingを使ったほうがいい
        val textView = findViewById<TextView>(R.id.activity_main_text_view)

        if (checkUsageStatsPermission()) {
            // 権限がある
            val byte = getMobileDataUsageFromCurrentMonth()
            // MBへ変換。Byte -> KB -> MB
            val usageMB = byte / 1024f / 1024f
            // TextViewに入れる
            textView.text = "$usageMB MB"
        } else {
            // ない
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }

    }

    /**
     * PACKAGE_USAGE_STATSの権限が付与されているか確認する
     * @return 権限があればtrue
     * */
    fun checkUsageStatsPermission(): Boolean {
        val appOpsManager = getSystemService(APP_OPS_SERVICE) as AppOpsManager
        val mode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            // Android 10 以降
            appOpsManager.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), application.packageName)
        } else {
            // Android 9 以前
            appOpsManager.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), application.packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /**
     * 今月のモバイルデータ利用量を取得する。単位はバイト
     * @return バイト単位で返す
     * */
    fun getMobileDataUsageFromCurrentMonth(): Long {
        val networkStatsManager = getSystemService(Context.NETWORK_STATS_SERVICE) as NetworkStatsManager
        // 集計開始の日付その月の最初の日
        val startTime = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }.time.time
        // 集計終了は現在時刻
        val endTime = Calendar.getInstance().time.time
        // 問い合わせる
        val bucket = networkStatsManager.querySummaryForDevice(ConnectivityManager.TYPE_MOBILE, null, startTime, endTime)
        // 送信 + 受信
        return bucket.txBytes + bucket.rxBytes
    }

}