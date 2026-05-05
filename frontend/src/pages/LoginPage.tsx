export function LoginPage() {
  const apiBase = import.meta.env.VITE_API_URL.replace(/\/$/, '')

  function signInWithGoogle() {
    window.location.href = `${apiBase}/api/v1/auth/oauth2/google`
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-slate-950 p-6 text-slate-100">
      <div className="w-full max-w-sm rounded-lg border border-slate-800 bg-slate-900 p-6 shadow-xl">
        <h1 className="text-lg font-semibold">Вход</h1>
        <p className="mt-2 text-sm text-slate-400">
          Используйте корпоративный аккаунт Google для доступа к сервису.
        </p>
        <button
          type="button"
          onClick={signInWithGoogle}
          className="mt-6 w-full rounded-md bg-white px-4 py-2.5 text-sm font-medium text-slate-900 transition hover:bg-slate-200"
        >
          Войти через Google
        </button>
      </div>
    </div>
  )
}
