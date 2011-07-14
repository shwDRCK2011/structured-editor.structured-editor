package ru.ipo.structurededitor;

import ru.ipo.structurededitor.actions.ActionsListComponent;
import ru.ipo.structurededitor.actions.VisibleElementAction;
import ru.ipo.structurededitor.model.DefaultDSLBean;
import ru.ipo.structurededitor.view.StructuredEditorModel;
import ru.ipo.structurededitor.view.StructuredEditorUI;
import ru.ipo.structurededitor.view.TextPosition;
import ru.ipo.structurededitor.view.VisibleElementsGraph;
import ru.ipo.structurededitor.view.elements.VisibleElement;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.InputStream;

/**
 * Created by IntelliJ IDEA. User: Ilya Date: 02.01.2010 Time: 14:52:43
 */
public class StructuredEditor extends JComponent implements Scrollable {

    private boolean view = false;

    private Object app;

    private StructuredEditorModel model;

    private ActionsListComponent actionsListComponent;

    public boolean isView() {
        return view;
    }

    public Object getApp() {
        return app;
    }

    public void setApp(Object app) {
        this.app = app;
        model.setApp(app);
    }

    public StructuredEditor() {
        this(new StructuredEditorModel(new DefaultDSLBean()));
    }

    public StructuredEditor(StructuredEditorModel model) {
        setModel(model);

        setFocusable(true);

        registerCaretMovementKeyStrokes();
        enableEvents(AWTEvent.MOUSE_EVENT_MASK);

        createActionsListComponent();
    }

    public StructuredEditor(StructuredEditorModel model, boolean view) {
        this(model);
        this.view = view;
        model.setView(view);
    }

    public StructuredEditorModel getModel() {
        return model;
    }

    public Dimension getPreferredScrollableViewportSize() {
        return ui.getPreferredSize(this);
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension parentDimension = getParent().getSize();
        parentDimension = parentDimension == null
                ? new Dimension(1, 1)
                : parentDimension;

        Dimension uiDimension = ui.getPreferredSize(this);
        uiDimension = uiDimension == null ? new Dimension(1, 1) : uiDimension;

        if (uiDimension.width > parentDimension.width)
            parentDimension.width = uiDimension.width;
        if (uiDimension.height > parentDimension.height)
            parentDimension.height = uiDimension.height;

        return parentDimension;
    }

    public int getScrollableBlockIncrement(Rectangle visibleRect,
                                           int orientation, int direction) {
        return getScrollableUnitIncrement(visibleRect, orientation, direction);
    }

    public boolean getScrollableTracksViewportHeight() {
        return false;
    }

    // implement Scrollable

    public boolean getScrollableTracksViewportWidth() {
        return false;
    }

    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        switch (orientation) {
            case SwingConstants.VERTICAL:
                return ((StructuredEditorUI) ui).getCharHeight();
            case SwingConstants.HORIZONTAL:
                return ((StructuredEditorUI) ui).getCharWidth();
        }
        return 0; // may not occur
    }

    public StructuredEditorUI getUI() {
        return (StructuredEditorUI) ui;
    }

    @Override
    protected void processComponentKeyEvent(KeyEvent e) {
        if (!view) {
            //let
            VisibleElement el = model.getFocusedElement();
            while (el != null) {
                el.fireKeyEvent(e);
                if (e.isConsumed())
                    return;
                el = el.getParent();
            }

            //fire action
            if (!e.isConsumed()) {
                VisibleElementAction action = actionsListComponent.getActionByKeyEvent(e);
                if (action != null)
                    action.run(model);
            }
        }
    }

    @Override
    protected void processMouseEvent(MouseEvent e) {
        if (!view) {
            VisibleElementsGraph graph = new VisibleElementsGraph(model
                    .getRootElement());
            int x = getUI().pixelsToX((e.getX()));
            int y = getUI().pixelsToY((e.getY()));

            TextPosition tp = model.getRootElement().getAbsolutePosition();
            int w = model.getRootElement().getWidth();
            int h = model.getRootElement().getHeight();
            if (e.getID() == MouseEvent.MOUSE_CLICKED && x <= tp.getColumn() + w && y <= tp.getLine() + h) {
                this.requestFocusInWindow();
                /*   if (app!=null){
                    try {
                       app.getClass().getSuperclass().getDeclaredMethod("clearSelectedGeos").invoke(app);
                    } catch (Exception ex) {
                        throw new Error(ex);
                    }
                }*/
                TextPosition p = graph.normalize(new TextPosition(y, x), VisibleElementsGraph.Direction.Down);
                x = p.getColumn();
                y = p.getLine();
                model.setAbsoluteCaretY(y);
                model.setAbsoluteCaretX(x);
                VisibleElement newFocused = graph.findElementByPos(x, y);
                if (newFocused == model.getFocusedElement())
                    model.repaint();
                else
                    model.setFocusedElement(newFocused);
                //VisibleElement el = model.getRootElement();
                e = new MouseEvent(
                        (Component) e.getSource(),
                        e.getID(),
                        e.getWhen(),
                        e.getModifiers(),
                        x,
                        y,
                        e.getClickCount(),
                        e.isPopupTrigger(),
                        e.getButton()
                );
                newFocused.fireMouseEvent(e);
            }
        }

    }

    private void registerCaretMovementKeyStrokes() {
        getInputMap().put(KeyStroke.getKeyStroke("pressed UP"), "move caret up");
        getInputMap()
                .put(KeyStroke.getKeyStroke("pressed DOWN"), "move caret down");
        getInputMap()
                .put(KeyStroke.getKeyStroke("pressed LEFT"), "move caret left");
        getInputMap().put(KeyStroke.getKeyStroke("pressed RIGHT"),
                "move caret right");
        getActionMap().put("move caret up",
                new CaretMovementAction(VisibleElementsGraph.Direction.Up));
        getActionMap().put("move caret down",
                new CaretMovementAction(VisibleElementsGraph.Direction.Down));
        getActionMap().put("move caret left",
                new CaretMovementAction(VisibleElementsGraph.Direction.Left));
        getActionMap().put("move caret right",
                new CaretMovementAction(VisibleElementsGraph.Direction.Right));
    }

    public void setModel(StructuredEditorModel model) {
        this.model = model;
        updateUI();
    }

    @Override
    public String getUIClassID() {
        return "StructuredEditorUI";
    }

    @Override
    public void updateUI() {
        setUI(UIManager.getUI(this));
    }

    private void createActionsListComponent() {
        actionsListComponent = new ActionsListComponent(this);
    }

    public ActionsListComponent getActionsListComponent() {
        return actionsListComponent;
    }

    //TODO this should be done when L&F is loading, now we don't load load L&F so this method must be called before any usage of Structured Editor
    public static void initializeStructuredEditorUI() {
        try {
            String[] fonts = {
                    "DejaVuSansMono-Bold.ttf",
                    "DejaVuSansMono-BoldOblique.ttf",
                    "DejaVuSansMono-Oblique.ttf",
                    "DejaVuSansMono.ttf"
            };
            for (String font : fonts) {
                InputStream fontStream = StructuredEditor.class.getResourceAsStream("resources/" + font);
                Font f = Font.createFont(Font.TRUETYPE_FONT, fontStream);
                GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(f);
                fontStream.close();
            }
        } catch (Exception e) {
            throw new Error("Failed to load fonts");
        }

        UIManager.put("StructuredEditorUI", "ru.ipo.structurededitor.view.StructuredEditorUI");
        UIManager.put("StructuredEditor.font", new Font(Font.MONOSPACED/*"DejaVu Sans Mono"*/, Font.PLAIN, 14));
        UIManager.put("StructuredEditor.horizontalMargin", 0);
        UIManager.put("StructuredEditor.verticalMargin", 0);
        UIManager.put("StructuredEditor.focusedColor", new Color(0xFFFF88));
        UIManager.put("StructuredEditor.textSelection.color", new Color(0x00FFFF));

        UIManager.put("StructuredEditor.text.edit.color", new Color(0x0000FF));
        UIManager.put("StructuredEditor.text.number.color", new Color(0x008800));

        UIManager.put("ActionsListComponent.background", new Color(0xEEEEEE));

        UIManager.put("AutoCompleteTextElement.unknownShortcut", new Color(0xDD3300));
        UIManager.put("AutoCompleteTextElement.knownShortcut", new Color(0x33DD00));
    }
}
