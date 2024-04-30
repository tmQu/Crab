package tkpm.com.crab.api

interface APICallback<T> {
    fun onSuccess(data: T)
    fun onError(error: Throwable)
}