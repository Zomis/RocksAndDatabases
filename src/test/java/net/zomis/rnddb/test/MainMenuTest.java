package net.zomis.rnddb.test;

import static org.junit.Assert.*;

import java.io.IOException;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.TextField;
import net.zomis.rnddb.main.MainController;

import org.junit.Test;
import org.loadui.testfx.GuiTest;

public class MainMenuTest extends GuiTest {
	
	public Parent getRootNode() {
		try {
			return FXMLLoader.load(MainController.class.getResource("rnddbStart.fxml"));
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	public void fillInAddress() {
		TextField address = find("#serverAddress");
		assertEquals("", address.getText());
		
		click(address).type("127.0.0.1");
		assertEquals("127.0.0.1", address.getText());
	}
}