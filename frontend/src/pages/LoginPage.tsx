import { useSearchParams } from 'react-router-dom'

const ERROR_MESSAGES: Record<string, string> = {
  missing_params: 'Google не передал параметры авторизации. Попробуйте ещё раз.',
  invalid_state: 'Сессия входа устарела. Повторите попытку.',
  invalid_nonce: 'Нарушена целостность входа (nonce mismatch). Повторите попытку.',
  no_id_token: 'Google не вернул id_token. Проверьте клиентские настройки OAuth.',
  server_error: 'Внутренняя ошибка сервера при входе. Проверьте логи backend.',
  google_access_denied: 'Вы отменили вход через Google.',
}

export function LoginPage() {
  const apiBase = import.meta.env.VITE_API_URL.replace(/\/$/, '')
  const [searchParams] = useSearchParams()
  const rawError = searchParams.get('error')
  const errorText = rawError
    ? ERROR_MESSAGES[rawError] ?? `Не удалось войти: ${rawError}`
    : null

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
        {errorText && (
          <p className="mt-4 rounded-md border border-red-700 bg-red-950/60 px-3 py-2 text-sm text-red-200">
            {errorText}
          </p>
        )}
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
