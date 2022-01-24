package AppiumSpider;

import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.DesiredCapabilities;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.TimeUnit;

public class AndroidBasicInteractionsTest {


    public static void main(String[] args) throws MalformedURLException, InterruptedException {
        DesiredCapabilities capabilities = new DesiredCapabilities();
        capabilities.setCapability("deviceName","SM_G9810");
        capabilities.setCapability("platformName","Android");
        capabilities.setCapability("platformVersion","11");
        capabilities.setCapability("appPackage", "com.rjh.rjhospital");
        capabilities.setCapability("appActivity", ".MainActivity");
        AndroidDriver<WebElement> driver = new
                AndroidDriver<WebElement>(new URL("http://127.0.0.1:4723/wd/hub"),capabilities);
        //driver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);
        Thread.sleep(36000);
        driver.quit();


    }
}
