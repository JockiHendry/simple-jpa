package $packageName

import org.jdesktop.swingx.JXStatusBar
import javax.swing.*
import javax.swing.border.*
import java.awt.*
import java.awt.event.*

actions {
<%
   domainClassLists.each { String name ->
        out << "\taction(id: '${GriffonUtil.getPropertyName(name)}', name: '${GriffonUtil.getNaturalName(name)}', actionCommandKey: '${GriffonUtil.getPropertyName(name)}', closure: controller.switchPage)\n"
   }
%>
}

application(title: 'Simple JPA Demo',
  extendedState: JFrame.MAXIMIZED_BOTH,
  pack: true,
  locationByPlatform: true) {

    borderLayout()

    toolBar(constraints: BorderLayout.PAGE_START, floatable: false) {
<%
    domainClassLists.each { String name ->
        out << "\t\tbutton(action: ${GriffonUtil.getPropertyName(name)}, verticalTextPosition: SwingConstants.BOTTOM, horizontalTextPosition: SwingConstants.CENTER)\n"
    }
%>
    }

    panel(id: "mainPanel", constraints: BorderLayout.CENTER) {
        cardLayout(id: "cardLayout")
    }

    statusBar(constraints: BorderLayout.PAGE_END, border: BorderFactory.createBevelBorder(BevelBorder.LOWERED)) {
        busyLabel(id: "busyLabel", busy: true, visible: false)
    }
}
