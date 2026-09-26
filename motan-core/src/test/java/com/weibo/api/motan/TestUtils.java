/*
 *
 *   Copyright 2009-2022 Weibo, Inc.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */

package com.weibo.api.motan;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Set;

/**
 * @author zhanglei28
 * @date 2022/8/2.
 */
public class TestUtils {
    public static Map<String, String> getModifiableEnvironment() throws Exception {
        Class<?> pe = Class.forName("java.lang.ProcessEnvironment");
        Method getenv = pe.getDeclaredMethod("getenv");
        getenv.setAccessible(true);
        Object unmodifiableEnvironment = getenv.invoke(null);
        Class<?> map = Class.forName("java.util.Collections$UnmodifiableMap");
        Field m = map.getDeclaredField("m");
        m.setAccessible(true);
        Map<String, String> env = (Map<String, String>) m.get(unmodifiableEnvironment);
        // On Windows, System.getenv(name) reads from theCaseInsensitiveEnvironment, so keep it in sync.
        Map<String, String> caseInsensitiveEnv = null;
        try {
            Field ci = pe.getDeclaredField("theCaseInsensitiveEnvironment");
            ci.setAccessible(true);
            caseInsensitiveEnv = (Map<String, String>) ci.get(null);
        } catch (NoSuchFieldException ignore) {
        }
        return caseInsensitiveEnv == null ? env : new SyncedEnvMap(env, caseInsensitiveEnv);
    }

    private static class SyncedEnvMap extends AbstractMap<String, String> {
        private final Map<String, String> primary;
        private final Map<String, String> secondary;

        SyncedEnvMap(Map<String, String> primary, Map<String, String> secondary) {
            this.primary = primary;
            this.secondary = secondary;
        }

        @Override
        public Set<Entry<String, String>> entrySet() {
            return primary.entrySet();
        }

        @Override
        public String get(Object key) {
            return primary.get(key);
        }

        @Override
        public boolean containsKey(Object key) {
            return primary.containsKey(key);
        }

        @Override
        public String put(String key, String value) {
            secondary.put(key, value);
            return primary.put(key, value);
        }

        @Override
        public String remove(Object key) {
            secondary.remove(key);
            return primary.remove(key);
        }

        @Override
        public void clear() {
            secondary.clear();
            primary.clear();
        }
    }
}
