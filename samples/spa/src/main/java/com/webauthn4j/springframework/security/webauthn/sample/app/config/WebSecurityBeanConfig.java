/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.webauthn4j.springframework.security.webauthn.sample.app.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import com.webauthn4j.WebAuthnManager;
import com.webauthn4j.converter.util.ObjectConverter;
import com.webauthn4j.metadata.converter.jackson.WebAuthnMetadataJSONModule;
import com.webauthn4j.springframework.security.webauthn.WebAuthnRegistrationRequestValidator;
import com.webauthn4j.springframework.security.webauthn.WebAuthnSecurityExpression;
import com.webauthn4j.springframework.security.webauthn.challenge.ChallengeRepository;
import com.webauthn4j.springframework.security.webauthn.challenge.HttpSessionChallengeRepository;
import com.webauthn4j.springframework.security.webauthn.options.OptionsProvider;
import com.webauthn4j.springframework.security.webauthn.options.OptionsProviderImpl;
import com.webauthn4j.springframework.security.webauthn.server.ServerPropertyProvider;
import com.webauthn4j.springframework.security.webauthn.server.ServerPropertyProviderImpl;
import com.webauthn4j.springframework.security.webauthn.userdetails.WebAuthnUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.security.web.access.DelegatingAccessDeniedHandler;
import org.springframework.security.web.authentication.*;
import org.springframework.security.web.authentication.logout.ForwardLogoutSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.csrf.InvalidCsrfTokenException;
import org.springframework.security.web.csrf.MissingCsrfTokenException;

import java.util.LinkedHashMap;

@Configuration
public class WebSecurityBeanConfig {

    @Bean
    public WebAuthnRegistrationRequestValidator webAuthnRegistrationRequestValidator(WebAuthnManager webAuthnManager, ServerPropertyProvider serverPropertyProvider) {
        return new WebAuthnRegistrationRequestValidator(webAuthnManager, serverPropertyProvider);
    }

    @Bean
    public AuthenticationTrustResolver authenticationTrustResolver() {
        return new AuthenticationTrustResolverImpl();
    }

    @Bean
    public ChallengeRepository challengeRepository() {
        return new HttpSessionChallengeRepository();
    }

    @Bean
    public OptionsProvider optionsProvider(WebAuthnUserDetailsService webAuthnUserDetailsService, ChallengeRepository challengeRepository) {
        return new OptionsProviderImpl(webAuthnUserDetailsService, challengeRepository);
    }

    @Bean
    public ServerPropertyProvider serverPropertyProvider(OptionsProvider optionsProvider, ChallengeRepository challengeRepository) {
        return new ServerPropertyProviderImpl(optionsProvider, challengeRepository);
    }

    @Bean
    public WebAuthnManager webAuthnManager(ObjectConverter objectConverter) {

        return WebAuthnManager.createNonStrictWebAuthnManager(objectConverter);
    }

    @Bean
    public WebAuthnSecurityExpression webAuthnSecurityExpression() {
        return new WebAuthnSecurityExpression();
    }


    @Bean
    public ObjectConverter objectConverter() {
        ObjectMapper jsonMapper = new ObjectMapper();
        jsonMapper.registerModule(new WebAuthnMetadataJSONModule());
        ObjectMapper cborMapper = new ObjectMapper(new CBORFactory());
        return new ObjectConverter(jsonMapper, cborMapper);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationSuccessHandler authenticationSuccessHandler() {
        return new ForwardAuthenticationSuccessHandler("/api/status/200");
    }

    @Bean
    public AuthenticationFailureHandler authenticationFailureHandler() {
        LinkedHashMap<Class<? extends AuthenticationException>, AuthenticationFailureHandler> authenticationFailureHandlers = new LinkedHashMap<>();

        // authenticator error handler
        ForwardAuthenticationFailureHandler authenticationFailureHandler = new ForwardAuthenticationFailureHandler("/api/status/401");
        authenticationFailureHandlers.put(AuthenticationException.class, authenticationFailureHandler);

        // default error handler
        AuthenticationFailureHandler defaultAuthenticationFailureHandler = new ForwardAuthenticationFailureHandler("/api/status/401");

        return new DelegatingAuthenticationFailureHandler(authenticationFailureHandlers, defaultAuthenticationFailureHandler);
    }

    @Bean
    public LogoutSuccessHandler logoutSuccessHandler() {
        return new ForwardLogoutSuccessHandler("/api/status/200");
    }

    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        LinkedHashMap<Class<? extends AccessDeniedException>, AccessDeniedHandler> errorHandlers = new LinkedHashMap<>();

        // invalid csrf authenticator error handler
        AccessDeniedHandlerImpl invalidCsrfTokenErrorHandler = new AccessDeniedHandlerImpl();
        invalidCsrfTokenErrorHandler.setErrorPage("/api/status/403");
        errorHandlers.put(InvalidCsrfTokenException.class, invalidCsrfTokenErrorHandler);

        // missing csrf authenticator error handler
        AccessDeniedHandlerImpl missingCsrfTokenErrorHandler = new AccessDeniedHandlerImpl();
        missingCsrfTokenErrorHandler.setErrorPage("/api/status/403");
        errorHandlers.put(MissingCsrfTokenException.class, missingCsrfTokenErrorHandler);

        // default error handler
        AccessDeniedHandlerImpl defaultErrorHandler = new AccessDeniedHandlerImpl();
        defaultErrorHandler.setErrorPage("/api/status/403");

        return new DelegatingAccessDeniedHandler(errorHandlers, defaultErrorHandler);
    }

    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        LoginUrlAuthenticationEntryPoint authenticationEntryPoint = new LoginUrlAuthenticationEntryPoint("/api/status/401");
        authenticationEntryPoint.setUseForward(true);
        return authenticationEntryPoint;
    }

}
