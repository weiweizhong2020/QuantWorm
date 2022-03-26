/* 
 * Filename: PlateView.java
 */
package org.quantworm.wormgender;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JRadioButton;

/**
 * ChooseWormGenderDialog.java
 *
 * is a JDialog for choosing how many males, and how many hermaphrodites
 */
public class ChooseWormGenderDialog extends JDialog {

    // a default version ID
    private static final long serialVersionUID = 1L;

    protected JButton okButton;

    protected JButton cancelButton;

    protected JRadioButton[] maleRadioButton;

    protected JRadioButton[] hermaRadioButton;

    protected String value;

    /**
     * Default constructor
     *
     * @param parentDialog
     * @param modal
     */
    public ChooseWormGenderDialog(JDialog parentDialog, boolean modal) {
        super(parentDialog, modal);
        setTitle("How many?");
        value = null;

        cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                value = null;
                setVisible(false);
            }
        });

        okButton = new JButton("OK");
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                String males = "0";
                String herma = "0";
                for (int i = 0; i < maleRadioButton.length; i++) {
                    if (maleRadioButton[ i].isSelected() == true) {
                        males = "" + i;
                    }
                    if (hermaRadioButton[ i].isSelected() == true) {
                        herma = "" + i;
                    }
                }
                value = males + "," + herma;
                setVisible(false);
            }
        });

        setLayout(new GridBagLayout());
        int gridyCount = 0;

        JLabel label = new JLabel(
                "<html><font color=gray><u>Choose number of males and/or hermaphrodites</u></font></html>",
                JLabel.CENTER);
        GridBagConstraints gbc0 = new GridBagConstraints();
        gbc0.gridx = 0;
        gbc0.gridy = gridyCount;
        gbc0.gridwidth = 2;
        gbc0.fill = GridBagConstraints.HORIZONTAL;
        gbc0.anchor = GridBagConstraints.CENTER;
        add(label, gbc0);
        gridyCount++;

        label = new JLabel("<html><b>males</b></html>", JLabel.CENTER);
        GridBagConstraints gbc1 = new GridBagConstraints();
        gbc1.gridx = 0;
        gbc1.gridy = gridyCount;
        gbc1.fill = GridBagConstraints.HORIZONTAL;
        gbc1.anchor = GridBagConstraints.CENTER;
        add(label, gbc1);

        label = new JLabel("<html><b>hermaphrodites</b></html>", JLabel.CENTER);
        GridBagConstraints gbc2 = new GridBagConstraints();
        gbc2.gridx = 1;
        gbc2.gridy = gridyCount;
        gbc2.fill = GridBagConstraints.HORIZONTAL;
        gbc2.anchor = GridBagConstraints.CENTER;
        add(label, gbc2);

        gridyCount++;

        maleRadioButton = new JRadioButton[9];
        hermaRadioButton = new JRadioButton[9];
        ButtonGroup maleButtonGroup = new ButtonGroup();
        ButtonGroup hermaButtonGroup = new ButtonGroup();
        int i = 0;
        for (JRadioButton radioButton : maleRadioButton) {
            radioButton = new JRadioButton("" + i, i == 0); // default males: 0
            maleButtonGroup.add(radioButton);
            maleRadioButton[ i] = radioButton;
            GridBagConstraints gbc3 = new GridBagConstraints();
            gbc3.gridx = 0;
            gbc3.gridy = gridyCount;
            add(radioButton, gbc3);

            radioButton = new JRadioButton("" + i, i == 1); // default hermaphrodites: 1
            hermaButtonGroup.add(radioButton);
            hermaRadioButton[ i] = radioButton;
            GridBagConstraints gbc4 = new GridBagConstraints();
            gbc4.gridx = 1;
            gbc4.gridy = gridyCount;
            add(radioButton, gbc4);

            gridyCount++;
            i++;
        }

        GridBagConstraints gbc7 = new GridBagConstraints();
        gbc7.gridx = 0;
        gbc7.gridy = gridyCount;
        gbc7.insets = new Insets(6, 6, 6, 6);
        gbc7.fill = GridBagConstraints.HORIZONTAL;
        add(cancelButton, gbc7);

        GridBagConstraints gbc8 = new GridBagConstraints();
        gbc8.gridx = 1;
        gbc8.gridy = gridyCount;
        gbc8.insets = new Insets(6, 6, 6, 6);
        gbc8.fill = GridBagConstraints.HORIZONTAL;
        add(okButton, gbc8);

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent windowEvent) {
                value = null;
                setVisible(false);
            }
        });

        pack();
    }

    /**
     * Returns the value
     *
     * @return the value
     */
    public String getValue() {
        return value;
    }

}
