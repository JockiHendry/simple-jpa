package simplejpa.swing

import groovy.text.SimpleTemplateEngine
import groovy.text.Template

import java.text.NumberFormat


class TemplateRenderer extends SimpleTemplateEngine {

    static Writable make(Template template, def value) {
        template.make([
            "value": value,
            "numberFormat": NumberFormat.getInstance().&format,
            "percentFormat": NumberFormat.getPercentInstance().&format,
            "currencyFormat": NumberFormat.getCurrencyInstance().&format,
            "titleCase": { it.replaceAll(/\b[a-z]/, {it.toUpperCase()}) }.&call
        ])
    }

}
