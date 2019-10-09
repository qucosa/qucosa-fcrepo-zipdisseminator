/*
 * Copyright (C) 2019 Saxon State and University Library Dresden (SLUB)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.qucosa.zipdisseminator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

class FileFilterBuilder {

    private final HashMap<String, String> extensions = new HashMap<>();
    private final HashSet<String> rejections = new HashSet<>();
    private final ArrayList<String[]> replacements = new ArrayList<>();

    FileFilterBuilder reject(String mimetype, String filename) {
        rejections.add(filename + "::" + mimetype);
        return this;
    }

    FileFilterBuilder appendMissingFileExtension(String mimetype, String extension) {
        extensions.put(mimetype, extension);
        return this;
    }

    FileFilterBuilder replaceAll(String regexp, String replace) {
        replacements.add(new String[]{regexp, replace});
        return this;
    }

    FileFilter build() {
        return new FileFilter() {
            @Override
            public boolean accepts(String filename, String mimetype) {
                return !rejections.contains(filename + "::" + mimetype);
            }

            @Override
            public String transformName(String filename, String mimetype) {
                String result = filename;
                for (String[] replacement : replacements) {
                    result = result.replaceAll(replacement[0], replacement[1]);
                }
                if (extensions.containsKey(mimetype)) {
                    if (!filename.matches("(.*)\\.(.+)$")) {
                        result += "." + extensions.get(mimetype);
                    }
                }
                return result;
            }
        };
    }

}
