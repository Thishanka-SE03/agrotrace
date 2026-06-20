package com.lnbti.agrotrace

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.Serializable

// Global Supabase client reference
object Supabase {
    lateinit var client: SupabaseClient
        internal set

    val isInitialized: Boolean
        get() = ::client.isInitialized
}

// Data model matching your Supabase table
@Serializable
data class DocumentRecord(
    val id: String? = null,
    val full_name: String = "",
    val document_number: String = "",
    val date_of_birth: String = "",
    val expiry_date: String = "",
    val address: String = "",
    val aligned_image_url: String? = null,
    val created_at: String? = null,
    val updated_at: String? = null
)

@Serializable
data class LandApprovalForm(
    val lot_no_for_seeds: String,
    val land_address: String? = null,
    val transplanted_date: String? = null,
    val form_date: String? = null,
    val form_id: Long? = 1
)

@Serializable
data class SeedActLotNo(
    val lot_no: String,
    val seed_act_registration_no: String
)

@Serializable
data class SeedActRegistration(
    val seed_act_no: String
)

// Repository for all database operations
class DocumentRepository {

    private val db = Supabase.client.postgrest

    // ── SEED ACT REGISTRATION ───────────────────────────────
    suspend fun ensureRegistrationExists(regNo: String): Result<Unit> {
        return try {
            db["seed_act_registrations"].upsert(SeedActRegistration(regNo))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── SEED ACT LOT NO ─────────────────────────────────────
    suspend fun ensureSeedLotNoExists(lotNo: String, regNo: String): Result<Unit> {
        return try {
            // Upsert (Insert if not exists)
            db["seed_act_lot_no"].upsert(SeedActLotNo(lotNo, regNo))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── LAND APPROVAL FORM ──────────────────────────────────
    suspend fun insertLandApprovalForm(form: LandApprovalForm): Result<String> {
        return try {
            val response = db["land_approval_form"]
                .insert(form) { select() }
                .decodeSingle<LandApprovalForm>()
            Result.success(response.lot_no_for_seeds)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── INSERT ──────────────────────────────────────────────
    suspend fun insertDocument(document: DocumentRecord): Result<String> {
        return try {
            val response = db["documents"]
                .insert(document) { select() }
                .decodeSingle<DocumentRecord>()
            Result.success(response.id ?: "unknown")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── SELECT ALL ──────────────────────────────────────────
    suspend fun getAllDocuments(): Result<List<DocumentRecord>> {
        return try {
            val response = db["documents"]
                .select { order("created_at", Order.DESCENDING) }
                .decodeList<DocumentRecord>()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── SELECT ONE ──────────────────────────────────────────
    suspend fun getDocument(id: String): Result<DocumentRecord?> {
        return try {
            val response = db["documents"]
                .select { filter { eq("id", id) } }
                .decodeSingleOrNull<DocumentRecord>()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── UPDATE ──────────────────────────────────────────────
    suspend fun updateDocument(id: String, updates: Map<String, Any?>): Result<Unit> {
        return try {
            db["documents"]
                .update(updates) { filter { eq("id", id) } }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── DELETE ──────────────────────────────────────────────
    suspend fun deleteDocument(id: String): Result<Unit> {
        return try {
            db["documents"]
                .delete { filter { eq("id", id) } }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
