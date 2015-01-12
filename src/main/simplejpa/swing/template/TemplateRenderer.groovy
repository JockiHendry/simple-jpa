/*
 * Copyright 2015 Jocki Hendry.
 *
 * Licensed under the Apache License, Version 2.0 (the 'License');
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an 'AS IS' BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */





package simplejpa.swing.template

import java.text.NumberFormat

class TemplateRenderer {

    String templateString
    Closure expression

    void add(String templateString) {
        this.templateString = templateString
    }

    void add(Closure expression) {
        this.expression = expression
    }

    String make(def value) {
        def result
        if (templateString) {
            String[] parts = templateString.split(':')
            String firstPart = parts[0].trim()
            if (firstPart.trim() == 'this') {
                result = value
            } else if (firstPart.trim().startsWith('#')) {
                result = value."${firstPart.substring(1)}"()
            } else {
                result = value."${firstPart}"
            }
            if (result != null) {
                for (int i = 1; i < parts.size(); i++) {
                    result = this."${parts[i].trim()}"(result)
                }
            }
        } else if (expression) {
            expression.delegate = this
            result = expression.call(value)
        } else {
            throw new IllegalArgumentException("TemplateRenderer doesn't have a template.")
        }
        result?: ''
    }

    String floatFormat(def v, int fracDigits) {
        NumberFormat nf = NumberFormat.getNumberInstance()
        nf.maximumFractionDigits = fracDigits
        nf.minimumFractionDigits = fracDigits
        nf.format(v)
    }

    String numberFormat(def v) {
        (v == null)? '': NumberFormat.getInstance().format(v)
    }

    String percentFormat(def v) {
        (v == null)? '': NumberFormat.getPercentInstance().format(v)
    }

    String currencyFormat(def v) {
        (v == null)? '': NumberFormat.getCurrencyInstance().format(v)
    }

    String lowerCase(def v) {
        (v == null)? '': (v as String).toLowerCase()
    }

    String upperCase(def v) {
        (v == null)? '': (v as String).toUpperCase()
    }

    String titleCase(def v) {
        (v == null)? '': (v as String).replaceAll(/\b[a-z]/, {v.toUpperCase()})
    }

}
