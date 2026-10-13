package com.example.lession_equipo1.network

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest

object SupabaseClient {
    val client = createSupabaseClient(
        supabaseUrl = "https://agcnpznjwzsukzvrmvrk.supabase.co",
        supabaseKey = "sb_publishable_xE2W7YWVEmOF0q_XygMViQ_pReB3oto"
    ) {
        install(Auth)
        install(Postgrest)
    }
}
