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

package simplejpa.swing;

import org.jdesktop.swingx.JXDatePicker;
import org.joda.time.*;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class DateTimePicker extends JPanel {

    JXDatePicker datePicker;
    JSpinner timeSpinner;
    SpinnerDateModel timeSpinnerModel;
    JSpinner.DateEditor timeSpinnerEditor;
    boolean dateVisible;
    boolean timeVisible;
    Color componentBackground;
    Closure selectedValueChanged;

    public DateTimePicker() {

        dateVisible = true
        timeVisible = true

        datePicker = new JXDatePicker()
        datePicker.setDate(DateTime.now().toDate())
        datePicker.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getPropertyName().equals("date")) {
                    firePropertyChange("localDate", null, getLocalDate())
                    firePropertyChange("localDateTime", null, getLocalDateTime())
                    firePropertyChange("dateTime", null, getDateTime())
                    firePropertyChange("dateMidnight", null, getDateMidnight())
                    selectedValueChanged?.call()
                }
            }
        })


        timeSpinnerModel = new SpinnerDateModel()
        timeSpinner = new JSpinner(timeSpinnerModel)
        timeSpinnerEditor = new JSpinner.DateEditor(timeSpinner, "HH:mm")
        timeSpinnerEditor.getTextField().setFont(datePicker.getFont())
        timeSpinnerEditor.getTextField().setBorder(BorderFactory.createEmptyBorder(3,3,3,3))
        timeSpinner.setEditor(timeSpinnerEditor)
        timeSpinnerModel.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                firePropertyChange("localTime", null, getLocalTime());
                firePropertyChange("localDateTime", null, getLocalDateTime());
                firePropertyChange("dateTime", null, getDateTime());
                selectedValueChanged?.call()
            }
        })

        componentBackground = Color.WHITE
        setDateVisible(dateVisible)
        setTimeVisible(timeVisible)

        setLayout(new FlowLayout(FlowLayout.LEADING, 0, 0))
        add(datePicker)
        add(timeSpinner)
    }

    public void setDateVisible(boolean dateVisible) {
        this.dateVisible = dateVisible;
        datePicker.setVisible(dateVisible);
    }

    public void setTimeVisible(boolean timeVisible) {
        this.timeVisible = timeVisible
        timeSpinner.setVisible(timeVisible)
        if (!timeVisible) {
            timeSpinnerModel.setValue(LocalTime.MIDNIGHT.toDateTimeToday().toDate())
        } else {
            timeSpinnerModel.setValue(LocalTime.now().toDateTimeToday().toDate())
        }
        firePropertyChange("localTime", null, getLocalTime())
        firePropertyChange("localDateTime", null, getLocalDateTime())
        firePropertyChange("dateTime", null, getDateTime())
    }

    public LocalDate getLocalDate() {
        return new LocalDate(datePicker.getDate())
    }

    public void setLocalDate(LocalDate localDate) {
        datePicker.setDate(localDate?.toDate() ?: LocalDate.now().toDate())
        firePropertyChange("localDate", null, localDate)
    }

    public LocalTime getLocalTime() {
        return new LocalTime(timeSpinnerModel.getDate())
    }

    public void setLocalTime(LocalTime localTime) {
        timeSpinnerModel.setValue(localTime?.toDateTimeToday()?.toDate() ?: LocalTime.now().toDateTimeToday().toDate())
        firePropertyChange("localTime", null, localTime)
    }

    public LocalDateTime getLocalDateTime() {
        return getLocalDate().toLocalDateTime(getLocalTime())
    }

    public void setLocalDateTime(LocalDateTime localDateTime) {
        datePicker.setDate(localDateTime?.toDate() ?: LocalDateTime.now().toDate())
        timeSpinnerModel.setValue(localDateTime?.toLocalTime()?.toDateTimeToday()?.toDate() ?: LocalTime.now().toDateTimeToday().toDate())
        firePropertyChange("localDateTime", null, localDateTime)
    }

    public DateMidnight getDateMidnight() {
        return new DateMidnight(datePicker.getDate())
    }

    public void setDateMidnight(DateMidnight dateMidnight) {
        datePicker.setDate(dateMidnight?.toDate() ?: DateMidnight.now().toDate())
        timeSpinnerModel.setValue(LocalTime.MIDNIGHT.toDateTimeToday().toDate())
        firePropertyChange("dateMidnight", null, dateMidnight)
    }

    public DateTime getDateTime() {
        return getLocalDate().toDateTime(getLocalTime())
    }

    public void setDateTime(DateTime dateTime) {
        datePicker.setDate(dateTime?.toDate() ?: DateTime.now().toDate())
        timeSpinner.setValue(dateTime?.toDate() ?: DateTime.now().toDate())

        firePropertyChange("dateTime", null, dateTime)
    }

    public void setComponentBackground(Color componentBackground) {
        datePicker?.editor.background = componentBackground
        if ("Nimbus".equals(UIManager.getLookAndFeel().getName())) {
            UIDefaults defaults = new UIDefaults()
            SolidColorPainter painter = new SolidColorPainter(componentBackground)
            defaults.put("Spinner:Panel:\"Spinner.formattedTextField\"[Enabled].backgroundPainter", painter)
            defaults.put("Spinner:Panel:\"Spinner.formattedTextField\"[Focused+Selected].backgroundPainter", painter)
            defaults.put("Spinner:Panel:\"Spinner.formattedTextField\"[Focused].backgroundPainter", painter)
            defaults.put("Spinner:Panel:\"Spinner.formattedTextField\"[Selected].backgroundPainter", painter)
            timeSpinnerEditor?.textField.putClientProperty("Nimbus.Overrides", defaults)
            timeSpinnerEditor?.textField.putClientProperty("Nimbus.Overrides.InheritDefaults", true)
            SwingUtilities.updateComponentTreeUI(timeSpinnerEditor?.textField)
        } else {
            timeSpinnerEditor?.textField?.background = componentBackground
        }
    }

    class SolidColorPainter implements Painter<JComponent> {

        Color color;

        public SolidColorPainter(Color color) {
            this.color = color
        }

        @Override
        public void paint(Graphics2D g, JComponent object, int width, int height) {
            g.setColor(Color.GRAY)
            g.drawRect(0, 0, width, height)
            g.setColor(color)
            g.fillRect(1, 1, width-1, height-1)
        }

    }

}