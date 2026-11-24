package com.communitycare.ai

import android.app.Application
import com.communitycare.ai.data.*
import com.google.gson.Gson
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

data class SyncStatus(val busy: Boolean = false, val message: String? = null, val checkedAt: String? = null)

class AppContainer(application: Application) {
    val storage = SecureStorage(application)
    val auth = AuthManager(application, storage)
    val encounters = EncounterStore(storage)
    val clinic = ClinicStore({ storage.read("clinic-records.bin") }, { storage.write("clinic-records.bin", it) })
    val syncMutex = Mutex()
    val syncStatus = MutableStateFlow(storage.read("sync-status.bin")?.let {
        Gson().fromJson(it, SyncStatus::class.java).copy(busy = false)
    } ?: SyncStatus())
    val api: BackendApi = Retrofit.Builder().baseUrl(BuildConfig.API_BASE_URL)
        .client(OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS).writeTimeout(30, TimeUnit.SECONDS)
            .followRedirects(false).followSslRedirects(false).build())
        .addConverterFactory(GsonConverterFactory.create()).build().create(BackendApi::class.java)

    val quality = QualityRepository(auth, api)

    fun remote(profile: Profile) = object : EncounterRemote {
        override suspend fun submit(item: LocalEncounter): Receipt {
            check(item.belongsTo(profile))
            return api.submit(item.id, "Bearer ${auth.accessToken(profile, api)}", item.orgUnit, item.submission).checkedBody()
        }
        override suspend fun receipt(item: LocalEncounter): Receipt {
            check(item.belongsTo(profile))
            return api.receipt(item.id, "Bearer ${auth.accessToken(profile, api)}", item.orgUnit).checkedBody()
        }
    }
}

class CommunityCareApplication : Application() {
    var container: AppContainer? = null
        private set
    var startupError: String? = null
        private set

    override fun onCreate() {
        super.onCreate()
        try {
            container = AppContainer(this)
            com.communitycare.ai.sync.EncounterSyncWorker.schedule(this, periodicOnly = true)
        } catch (error: Exception) {
            startupError = "Cannot open encrypted app data. Do not clear app storage; contact your administrator."
        }
    }
}
