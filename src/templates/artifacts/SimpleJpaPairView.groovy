package ${g.targetPackageName}

import net.miginfocom.swing.MigLayout
import org.joda.time.*
import java.awt.*

application(title: '${g.domainClassNameAsNatural}',
	preferredSize: [520, 340],
	pack: true,
	locationByPlatform: true,
	iconImage: imageIcon('/griffon-icon-48x48.png').image,
	iconImages: [imageIcon('/griffon-icon-48x48.png').image,
		imageIcon('/griffon-icon-32x32.png').image,
		imageIcon('/griffon-icon-16x16.png').image]) {

	panel(id: 'mainPanel') {
		borderLayout()

		panel(id: "form", layout: new MigLayout('', '[right][left][left,grow]',''), constraints: CENTER, focusCycleRoot: true) {
${g.dataEntry(3)}
			panel(constraints: 'span, growx, wrap') {
				flowLayout(alignment: FlowLayout.LEADING)
				button(app.getMessage("simplejpa.dialog.update.button"), actionPerformed: {
					if (model.${g.domainClassNameAsProperty}!=null) {
						if (JOptionPane.showConfirmDialog(mainPanel, app.getMessage("simplejpa.dialog.update.message"),
							app.getMessage("simplejpa.dialog.update.title"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) {
								return
						}
					}
					controller.save()
				})
				button(app.getMessage("simplejpa.dialog.delete.button"), visible: bind {model.${g.domainClassNameAsProperty}!=null},
					actionPerformed: {
						if (JOptionPane.showConfirmDialog(mainPanel, app.getMessage("simplejpa.dialog.delete.message"),
							app.getMessage("simplejpa.dialog.delete.title"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION) {
								controller.delete()
						}
				})
				button(app.getMessage("simplejpa.dialog.close.button"), actionPerformed: { controller.close() })
			}
		}
	}
}