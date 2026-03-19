/*
 * MIT License
 *
 * Copyright (c) 2026 AuraJewels (Raviraj Bhosale)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.aurajewels.jewel.security;

import com.aurajewels.jewel.exception.ModuleNotEnabledException;
import com.aurajewels.jewel.repository.StoreFeatureModuleRepository;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * AOP aspect that enforces feature module gating. If a controller method is annotated with {@link
 * RequiresModule}, this aspect checks whether that module is enabled for the current store (from
 * {@link StoreContext}). Super admins bypass this check.
 *
 * @author Raviraj Bhosale
 */
@Aspect
@Component
@RequiredArgsConstructor
public class ModuleAspect {

    private final StoreFeatureModuleRepository storeFeatureModuleRepository;

    @Around("@annotation(requiresModule)")
    public Object checkModule(ProceedingJoinPoint joinPoint, RequiresModule requiresModule)
            throws Throwable {

        String role = StoreContext.getCurrentRole();

        // Super admins and support bypass module checks
        if ("SUPER_ADMIN".equals(role) || "SUPPORT".equals(role)) {
            return joinPoint.proceed();
        }

        // Customer-app endpoints don't need module checks
        if ("CUSTOMER".equals(role)) {
            return joinPoint.proceed();
        }

        Long storeId = StoreContext.getCurrentStoreId();
        if (storeId == null) {
            // No store context — let it through (will fail on other checks)
            return joinPoint.proceed();
        }

        String requiredModule = requiresModule.value();

        boolean enabled =
                storeFeatureModuleRepository.existsByStoreIdAndModuleCodeAndEnabledTrue(
                        storeId, requiredModule);

        if (!enabled) {
            throw new ModuleNotEnabledException(requiredModule);
        }

        return joinPoint.proceed();
    }
}
