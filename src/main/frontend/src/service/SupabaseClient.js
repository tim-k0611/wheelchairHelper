import { createClient } from '@supabase/supabase-js';

// Fallback für Development (wenn .env nicht geladen wird)
const supabaseUrl = import.meta.env.VITE_SUPABASE_URL;
const supabaseAnonKey = import.meta.env.VITE_SUPABASE_ANON_KEY;

console.log('Supabase URL:', supabaseUrl); // Debug

if (!supabaseUrl || supabaseUrl.includes('your-project')) {
    console.error('⚠️ Supabase URL nicht konfiguriert! Bitte .env.local erstellen.');
}

export const supabase = createClient(supabaseUrl, supabaseAnonKey);

// Helper für Auth
export const authHelpers = {
    // Anmelden
    async signIn(email, password) {
        const { data, error } = await supabase.auth.signInWithPassword({
            email,
            password,
        });
        return { data, error };
    },

    // Registrieren
    async signUp(email, password) {
        const { data, error } = await supabase.auth.signUp({
            email,
            password,
        });
        return { data, error };
    },

    // Abmelden
    async signOut() {
        const { error } = await supabase.auth.signOut();
        return { error };
    },

    // Aktuellen User abrufen
    async getUser() {
        const { data: { user }, error } = await supabase.auth.getUser();
        return { user, error };
    },

    // Auth Session abrufen
    async getSession() {
        const { data: { session }, error } = await supabase.auth.getSession();
        return { session, error };
    },

    // Auth State Changes abonnieren
    onAuthStateChange(callback) {
        return supabase.auth.onAuthStateChange(callback);
    }
};