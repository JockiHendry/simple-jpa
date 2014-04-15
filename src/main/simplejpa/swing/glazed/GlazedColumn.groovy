/*
 * Copyright 2013 Jocki Hendry.
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

package simplejpa.swing.glazed

import groovy.beans.Bindable
import javax.swing.event.SwingPropertyChangeSupport
import javax.swing.table.TableColumn
import java.beans.PropertyChangeListener


class GlazedColumn extends TableColumn {

    public GlazedColumn() {
        super()
    }

    String name
    Closure expression
    String property
    Class columnClass
    Comparator comparator
    @Bindable Boolean visible = true

    //
    // @Bindable can't generate this because parent already has these methods,
    // but we can't use them because some of them are declared private by parents.
    // So just copied from parent to this here.
    //
    private SwingPropertyChangeSupport changeSupport

    public synchronized void addPropertyChangeListener(PropertyChangeListener listener) {
        super.addPropertyChangeListener(listener)
        if (changeSupport == null) changeSupport = new SwingPropertyChangeSupport(this)
        changeSupport.addPropertyChangeListener(listener)
    }

    public synchronized void removePropertyChangeListener(PropertyChangeListener listener) {
        super.removePropertyChangeListener(listener)
        if (changeSupport != null) {
            changeSupport.removePropertyChangeListener(listener);
        }
    }

    public synchronized PropertyChangeListener[] getPropertyChangeListeners() {
        if (changeSupport == null) return new PropertyChangeListener[0]
        return changeSupport.getPropertyChangeListeners()
    }


    private void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
        if (changeSupport != null) {
            changeSupport.firePropertyChange(propertyName, oldValue, newValue);
        }
    }

    private void firePropertyChange(String propertyName, int oldValue, int newValue) {
        if (oldValue != newValue) {
            firePropertyChange(propertyName, Integer.valueOf(oldValue), Integer.valueOf(newValue));
        }
    }

    private void firePropertyChange(String propertyName, boolean oldValue, boolean newValue) {
        if (oldValue != newValue) {
            firePropertyChange(propertyName, Boolean.valueOf(oldValue), Boolean.valueOf(newValue));
        }
    }

    //
    // End of property change listener
    //

}
