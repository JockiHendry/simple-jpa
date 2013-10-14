package simplejpa.swing

import groovy.text.SimpleTemplateEngine
import groovy.text.Template

import java.text.NumberFormat


class TemplateRenderer extends SimpleTemplateEngine {

    static String make(Template template, def value) {
        StringWriter result = new StringWriter()
        template.make([
            "it": value,
            "value": value,
            "numberFormat": this.&numberFormat,
            "floatFormat": this.&floatFormat,
            "percentFormat": this.&percentFormat,
            "currencyFormat": this.&currencyFormat,
            "titleCase": this.&titleCase
        ]).writeTo(result)
        result.flush()
        result.toString()
    }

    static String make(Closure closure, def value) {
        closure.delegate = this
        closure.call(value)
    }

    static String floatFormat(def v, int fracDigits) {
        NumberFormat nf = NumberFormat.getNumberInstance()
        nf.maximumFractionDigits = fracDigits
        nf.minimumFractionDigits = fracDigits
        nf.format(v)
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
