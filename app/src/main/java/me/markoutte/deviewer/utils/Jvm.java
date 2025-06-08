/*
 * Copyright 2025 Maksim Pelevin and contributors
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

package me.markoutte.deviewer.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Jvm {

    private static final Pattern pattern = Pattern.compile("\\((L.+;|V|Z|B|C|S|I|J|F|D)*\\)(L.+;|V|Z|B|C|S|I|J|F|D)");

    /**
     * Converts (ZBLjava/lang/Object;)V into list of ["boolean", "byte", "java.lang.Object", "void"]
     * */
    public static List<String> jvmNameToCanonical(String name) {
        Matcher matcher = pattern.matcher(name);
        if (matcher.matches()) {
            var parameters = name.toCharArray();
            List<String> list = new ArrayList<>();
            for (int i = 0; i < parameters.length; i++) {
                switch (parameters[i]) {
                    case 'V': list.add("void"); break;
                    case 'Z': list.add("boolean"); break;
                    case 'B': list.add("byte"); break;
                    case 'C': list.add("char"); break;
                    case 'S': list.add("short"); break;
                    case 'I': list.add("int"); break;
                    case 'J': list.add("long"); break;
                    case 'F': list.add("float"); break;
                    case 'D': list.add("double"); break;
                    case 'L': {
                        var j = i + 1;
                        while (parameters[j] != ';') {
                            j++;
                        }
                        char[] className = Arrays.copyOfRange(parameters, i + 1, j);
                        for (int c = 0; c < className.length; c++) {
                            if (className[c] == '/') {
                                className[c] = '.';
                            }
                        }
                        list.add(new String(className));
                        i = j;
                    }
                }
            }
            return list;
        }
        var list = new ArrayList<String>(1);
        list.add(name);
        return list;
    }
}
