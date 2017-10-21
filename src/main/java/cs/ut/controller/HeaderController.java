package cs.ut.controller;

import cs.ut.config.HeaderItem;
import cs.ut.config.MasterConfiguration;
import org.zkoss.util.resource.Labels;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.event.SerializableEventListener;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zkmax.zul.Navbar;
import org.zkoss.zkmax.zul.Navitem;

import java.util.Comparator;
import java.util.List;

/**
 * Controller class that is responsible for controls in the header of the page
 */

public class HeaderController extends SelectorComposer<Component> {

    @Wire
    Navbar navbar;

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);

        composeHeader();
    }

    /**
     * Constructs header items based on parameters defined in configuration.xml
     */
    private void composeHeader() {
        List<HeaderItem> items = MasterConfiguration.getInstance().getHeaderItems();
        items.sort(Comparator.comparing(HeaderItem::getPosition));

        items.forEach(it -> {
            Navitem navitem = new Navitem();
            navitem.setLabel(Labels.getLabel(it.getLabel()));
            navitem.addEventListener(Events.ON_CLICK, new SerializableEventListener<Event>() {
                @Override
                public void onEvent(Event event) throws Exception {
                    MainPageController.getInstance().setContent(it.getRedirect(), getPage());
                    navbar.selectItem(navitem);
                }
            });

            navitem.setDisabled(!it.isEnabled());
            navitem.setSclass("navbar-item");

            navbar.appendChild(navitem);
        });
    }


    @Listen("onClick = #headerLogo")
    public void handleClick() {
        MainPageController.getInstance().setContent("landing", getPage());
        navbar.setSelectedItem(null);
    }
}
