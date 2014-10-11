package ${g.targetPackageName}

import net.miginfocom.swing.MigLayout
import org.joda.time.*
import java.awt.*

actions {
	action(id: 'save', name: app.getMessage('simplejpa.dialog.update.button'), closure: controller.save)
	action(id: 'delete', name: app.getMessage("simplejpa.dialog.delete.button"), closure: controller.delete)
	action(id: 'close', name: app.getMessage("simplejpa.dialog.close.button"), closure: controller.close)
${g.actions(1)}}

panel(id: 'mainPanel') {
    borderLayout()

    panel(id: "form", layout: new MigLayout('', '[right][left][left,grow]',''), constraints: CENTER, focusCycleRoot: true) {
${g.dataEntry(3, false)}
        panel(constraints: 'span, growx, wrap') {
            flowLayout(alignment: FlowLayout.LEADING)
            button(action: save)
            button(visible: bind {model.${g.domainClassNameAsProperty}!=null}, action: delete)
            button(action: close)
        }
    }
}