package pl.edu.agh.msc.auth

import com.google.inject.name.Named
import com.google.inject.{ AbstractModule, Provides }
import com.mohiva.play.silhouette.api.actions.{ SecuredErrorHandler, UnsecuredErrorHandler }
import com.mohiva.play.silhouette.api.crypto._
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.services._
import com.mohiva.play.silhouette.api.util._
import com.mohiva.play.silhouette.api.{ Environment, EventBus, Silhouette, SilhouetteProvider }
import com.mohiva.play.silhouette.crypto.{ JcaCrypter, JcaCrypterSettings, JcaSigner, JcaSignerSettings }
import com.mohiva.play.silhouette.impl.authenticators._
import com.mohiva.play.silhouette.impl.providers._
import com.mohiva.play.silhouette.impl.util._
import com.mohiva.play.silhouette.password.{ BCryptPasswordHasher, BCryptSha256PasswordHasher }
import com.mohiva.play.silhouette.persistence.daos.{ DelegableAuthInfoDAO, InMemoryAuthInfoDAO }
import com.mohiva.play.silhouette.persistence.repositories.DelegableAuthInfoRepository
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ArbitraryTypeReader._
import net.codingwell.scalaguice.ScalaModule
import pl.edu.agh.msc.auth.controllers.handlers.{ CustomSecuredErrorHandler, CustomUnsecuredErrorHandler }
import pl.edu.agh.msc.auth.infra.DefaultEnv
import pl.edu.agh.msc.auth.jobs.{ AuthTokenCleaner, Scheduler }
import pl.edu.agh.msc.auth.passwords.PasswordRepository
import pl.edu.agh.msc.auth.token.AuthTokenService
import pl.edu.agh.msc.auth.user.{ UserRepository, UserService }
import play.api.Configuration
import play.api.libs.concurrent.AkkaGuiceSupport
import play.api.libs.ws.WSClient
import play.api.mvc.CookieHeaderEncoding

import scala.concurrent.ExecutionContext

class AuthModule extends AbstractModule with ScalaModule with AkkaGuiceSupport {

  def configure(): Unit = {
    bind[UserRepository].asEagerSingleton
    bind[AuthTokenService].asEagerSingleton
    bind[UserService].asEagerSingleton
    bind[DelegableAuthInfoDAO[PasswordInfo]].to[PasswordRepository].asEagerSingleton

    bindActor[AuthTokenCleaner]("auth-token-cleaner")
    bind[Scheduler].asEagerSingleton

    bind[Silhouette[DefaultEnv]].to[SilhouetteProvider[DefaultEnv]].asEagerSingleton
    bind[UnsecuredErrorHandler].to[CustomUnsecuredErrorHandler].asEagerSingleton
    bind[SecuredErrorHandler].to[CustomSecuredErrorHandler].asEagerSingleton
    bind[CacheLayer].to[PlayCacheLayer].asEagerSingleton
    bind[IDGenerator].to[SecureRandomIDGenerator].asEagerSingleton
    bind[FingerprintGenerator].toInstance(new DefaultFingerprintGenerator(false))
    bind[EventBus].toInstance(EventBus())
    bind[Clock].toInstance(Clock())
  }

  @Provides
  def provideHTTPLayer(client: WSClient)(implicit ec: ExecutionContext): HTTPLayer = new PlayHTTPLayer(client)

  @Provides
  def provideEnvironment(
    userService:          UserService,
    authenticatorService: AuthenticatorService[CookieAuthenticator],
    eventBus:             EventBus
  )(implicit ec: ExecutionContext): Environment[DefaultEnv] = {
    Environment[DefaultEnv](
      userService,
      authenticatorService,
      Seq(),
      eventBus
    )
  }

  @Provides @Named("authenticator-signer")
  def provideAuthenticatorSigner(configuration: Configuration): Signer = {
    val config = configuration.underlying.as[JcaSignerSettings]("silhouette.authenticator.signer")
    new JcaSigner(config)
  }

  @Provides @Named("authenticator-crypter")
  def provideAuthenticatorCrypter(configuration: Configuration): Crypter = {
    val config = configuration.underlying.as[JcaCrypterSettings]("silhouette.authenticator.crypter")
    new JcaCrypter(config)
  }

  @Provides
  def provideAuthInfoRepository(
    passwordInfoDAO: DelegableAuthInfoDAO[PasswordInfo]
  )(implicit ec: ExecutionContext): AuthInfoRepository = {
    new DelegableAuthInfoRepository(passwordInfoDAO)
  }

  @Provides
  def provideAuthenticatorService(
    @Named("authenticator-signer") signer:   Signer,
    @Named("authenticator-crypter") crypter: Crypter,
    cookieHeaderEncoding:                    CookieHeaderEncoding,
    fingerprintGenerator:                    FingerprintGenerator,
    idGenerator:                             IDGenerator,
    configuration:                           Configuration,
    clock:                                   Clock
  )(implicit ec: ExecutionContext): AuthenticatorService[CookieAuthenticator] = {
    val config = configuration.underlying.as[CookieAuthenticatorSettings]("silhouette.authenticator")
    val authenticatorEncoder = new CrypterAuthenticatorEncoder(crypter)

    new CookieAuthenticatorService(config, None, signer, cookieHeaderEncoding, authenticatorEncoder, fingerprintGenerator, idGenerator, clock)
  }

  @Provides
  def providePasswordHasherRegistry(): PasswordHasherRegistry = {
    PasswordHasherRegistry(new BCryptSha256PasswordHasher(), Seq(new BCryptPasswordHasher()))
  }

  @Provides
  def provideCredentialsProvider(
    authInfoRepository:     AuthInfoRepository,
    passwordHasherRegistry: PasswordHasherRegistry
  )(implicit ec: ExecutionContext): CredentialsProvider = {
    new CredentialsProvider(authInfoRepository, passwordHasherRegistry)
  }

  @Provides
  def provideSecureRandomIDGenerator(implicit ec: ExecutionContext): SecureRandomIDGenerator = {
    new SecureRandomIDGenerator()
  }

}
