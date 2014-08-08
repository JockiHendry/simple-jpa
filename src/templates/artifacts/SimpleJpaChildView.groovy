package ${g.targetPackageName}

import static ca.odell.glazedlists.gui.AbstractTableComparatorChooser.*
import static javax.swing.SwingConstants.*
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

		panel(constraints: CENTER) {
			borderLayout()
			panel(constraints: PAGE_START, layout: new FlowLayout(FlowLayout.LEADING)) {
				label(text: bind('searchMessage', source: model))
			}
			scrollPane(constraints: CENTER) {
				glazedTable(id: 'table', list: model.${g.domainClassGlazedListVariable}, sortingStrategy: SINGLE_COLUMN, onValueChanged: controller.tableSelectionChanged) {
${g.table(5)}
				}
			}
		}

		panel(id: "form", layout: new MigLayout('', '[right][left][left,grow]',''), constraints: PAGE_END, focusCycleRoot: true) {
${g.dataEntry(3)}

			panel(constraints: 'span, growx, wrap') {
				flowLayout(alignment: FlowLayout.LEADING)
				button(app.getMessage("simplejpa.dialog.update.button"), actionPerformed: {
					if (!view.table.selectionModel.selectionEmpty) {
						if (JOptionPane.showConfirmDialog(mainPanel, app.getMessage("simplejpa.dialog.update.message"),
							app.getMessage("simplejpa.dialog.update.title"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) {
								return
						}
					}
					controller.save()
					form.getFocusTraversalPolicy().getFirstComponent(form).requestFocusInWindow()
				})
				button(app.getMessage("simplejpa.dialog.cancel.button"), visible: bind{table.isRowSelected}, actionPerformed: controller.clear)
				button(app.getMessage("simplejpa.dialog.delete.button"), visible: bind{table.isRowSelected}, actionPerformed: {
					if (JOptionPane.showConfirmDialog(mainPanel, app.getMessage("simplejpa.dialog.delete.message"),
						app.getMessage("simplejpa.dialog.delete.title"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION) {
							controller.delete()
					}
				})
				button(app.getMessage("simplejpa.dialog.close.button"), actionPerformed: {
					SwingUtilities.getWindowAncestor(mainPanel)?.dispose()
				})
			}
		}
	}
}
