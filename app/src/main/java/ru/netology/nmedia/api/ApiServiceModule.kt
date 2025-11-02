package ru.netology.nmedia.api

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ru.netology.nmedia.auth.AppAuth
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object ApiServiceModule {

    @Provides
    @Singleton
    fun provideApiService(auth: AppAuth): PostApiService {
        return retrofit(
            client(
                loggingInterceptor(),
                authInterceptor(auth)
            )
        ).create(PostApiService::class.java)
    }

}