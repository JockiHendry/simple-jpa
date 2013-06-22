package simplejpa.swing

import groovy.text.SimpleTemplateEngine
import groovy.text.Template

import java.text.NumberFormat


class TemplateRenderer extends SimpleTemplateEngine {

    static Writable make(Template template, def value) {
        template.make([
            "value": value,
            "numberFormat": this.&numberFormat,
            "percentFormat": this.&percentFormat,
            "currencyFormat": this.&currencyFormat,
            "titleCase": this.&titleCase
        ])
    }

    static Writable make(Closure closure, def value) {
        closure.delegate = this
        closure.call(value)
    }

    static String numberFormat(def v) {
        NumberFormat.getInstance().format(v)
    }

    static String percentFormat(def v) {
        NumberFormat.getPercentInstance().format(v)
    }

    static String currencyFormat(def v) {
        NumberFormat.getCurrencyInstance().format(v)
    }

    static String titleCase(String v) {
        v.replaceAll(/\b[a-z]/, {v.toUpperCase()})
    }
}
