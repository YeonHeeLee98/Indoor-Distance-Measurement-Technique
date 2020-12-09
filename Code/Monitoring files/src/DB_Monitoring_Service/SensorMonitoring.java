package DB_Monitoring_Service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import DB_Monitoring_Service.Custom_Data_Type.DeviceInfo;


/*
 * �������� 
 * App �������� ���� ���̻��� push service�� �ʿ� ��������. 
 * Push service ��ɵ��� ��� off �ؾ� �� ���� ����͸������� ���� ������ ������ �µ� ���� ������ Firebase�� ����
 * 
 */
public class SensorMonitoring extends Thread
{
    // private PushService ps;
    private double limitTemperature;
    private int count = 0;
    private int accuracyCount = 0;
    List<DeviceInfo> deviceInfo = new ArrayList<DeviceInfo>();
    private boolean duplication = false;
    double avgAccuracy = 0.0;
    FirebaseStorage fbs;

    protected static double calculateDistance(int txPower, double rssi) {
    	  if (rssi == 0) {
    	    return -1.0; // if we cannot determine distance, return -1.
    	  }

    	  double ratio = rssi*1.0/txPower;
    	  if (ratio < 1.0) {
    	    return Math.pow(ratio,10);
    	  }
    	  else {
    	    double accuracy =  (0.89976)*Math.pow(ratio,7.7095) + 0.111;    
    	    return accuracy;
    	  }
    	} 
    
    public SensorMonitoring(double limitTemperature)
    {
        // ps = new PushService();
        this.limitTemperature = limitTemperature;
        
        // �����ڰ� ��������� firebase�� ���� 
        fbs = new FirebaseStorage();
        fbs.FirebaseConnet();        
    }


    public void run()
    {
        mysql m = new mysql();
        Double temperature = 0.0;
        Double humidity = 0.0;
        Double illumination = 0.0;

        while (true)
        {
            temperature = m.select_recent_temperature();    // ���� ���� ����
            humidity = m.select_recent_humidity();          //
            illumination = m.select_recent_illumination();  //

            // ���� ���� ���
            System.out.println("----------------\t ���� ���� ��� \t----------------");
            System.out.print("Temperature : " + temperature + ", ");
            System.out.print("humidity : " + humidity + ", ");
            System.out.println("illumination : " + illumination + ", ");
            System.out.println();

            // ����̽� ����
            // [0] MAC, [1] RSSI, [2] TX, [3] Accuracy, time
            System.out.println("----------------\t DB ����̽� ���� ��� \t----------------");
            String[] strDevice = m.device_variable_info_tb().split(" ");        // �о�� DB ������ �����ؼ� ����    
            for (int i = 0; i < strDevice.length; i++)
            {
                System.out.print(strDevice[i] + " ");       					// �о���� ������ �ϳ��� ���
            }            
            System.out.println();
            
            // UUID major minor ���
            // [0] UUID, [1] major, [2] minor
            System.out.println("UUID major minor: " + m.select_device_uuid(strDevice[0]));
            String[] uniqueDevice = m.select_device_uuid(strDevice[0]).split(" ");          
            System.out.println();
            
            // ù��°�� ���ư��� ��� ����Ʈ�� �ƹ��͵� ���� ������ �ٷ� ����Ʈ�� �߰�
            if (count == 0)
            {
                DeviceInfo first = new DeviceInfo();        // ����̽� ��ü Ŭ���� ����
                first.setMacAddress(strDevice[0]);          // MAC �ּ� �߰�
                first.setAccuracy(strDevice[3]);            // Accuracy �� �߰�
                first.setUUID(uniqueDevice[0]);             // UUID
                first.setMajor(uniqueDevice[1]);            // Major
                first.setMinor(uniqueDevice[2]);            // Minor                
                deviceInfo.add(first);                      // �ش� ��ü�� ��ü ����Ʈ�� �߰�
                count++;
            }
            
            /*
             * ����Ʈ�� ����̽� ���� ����
             * ��ü ����Ʈ�� ����Ͽ� ����̽��� �߰� �ɶ����� ���� �Ҵ� 
             * DB���� ���� �������� �о�� ������ MAC�� ������� �� �� �߰�
             * ���� ������ MAC �����Ͱ� �о������� Accuracy ������ �߰�
             * MAC �ּҰ� �ٸ� ��� ����Ʈ�� ���ο� ����̽� ���� �� �߰� 
             */          
            System.out.println("----------------\t MAC Compare \t----------------");
            for(int i = 0 ; i < deviceInfo.size(); i++)
            {   
                DeviceInfo DevinfoMacCompare = deviceInfo.get(i);                           // Devinfo�� ����� ��ü ������� ȣ��
                System.out.println("Device MAC \t: " + DevinfoMacCompare.getMacAddress());    
                System.out.println("Serch Dev MAC \t: " + strDevice[0]);
                // MAC �ּҰ� ���� ���
                if(DevinfoMacCompare.getMacAddress().equals(strDevice[0]))                  // ȣ��� DeviceInfo Ŭ������ MAC �Ķ���� ��
                {
                    System.out.println("MAC �ּҰ� ����");
                    DevinfoMacCompare.setAccuracy(strDevice[3]);   	// �ش� ����̽� ��ü Ŭ������ Accuracy �� �߰�
                    DevinfoMacCompare.setAccuracy(strDevice[3]);   	
                    duplication = true;                             // �ش� MAC ������ �̹� �ߺ��Ǿ� �ִٴ� ���� �˸��� ���� flag ��
                    System.out.println("Accuary �� �߰�  : " + DevinfoMacCompare.getNumAccuracy(DevinfoMacCompare.getAccuracy().size()-1));
                }
                System.out.println();
            }
            
            // ����Ʈ�� �� �˻��ߴµ��� �浹���� MAC �ּҰ� ���� ��� �ش� ����̽��� �߰��Ѵ�.
            // �̴� ������ ���� flag ���� true �� ��� ���� MAC���� false�� �ٸ� MAC �ΰ����� �Ǵ� 
            if(duplication == false)
            {
                System.out.println("MAC �ּҰ� �ٸ�");
                DeviceInfo newDev = new DeviceInfo();   // ���� �߰��� ����̽��� ���� ��ü ����
                newDev.setMacAddress(strDevice[0]);     // MAC �ּ� �߰�
                newDev.setAccuracy(strDevice[3]);       // Accuracy �߰�
                newDev.setUUID(uniqueDevice[0]);             // UUID
                newDev.setMajor(uniqueDevice[1]);            // Major
                newDev.setMinor(uniqueDevice[2]);            // Minor   
                deviceInfo.add(newDev);                 // ����̽� ����Ʈ�� �߰�
                System.out.println(newDev.getMacAddress() + " Device �߰�");
            }
            duplication = false;        // ture ���� ��츦 ���� �� �缳��
            System.out.println();
            
            
            // ����̽����� ������ Ȯ���ϱ� ���� ��ü ����Ʈ ���
            System.out.println("----------------\t ����̽� ���� ����Ʈ \t----------------");
            for (int i = 0; i < deviceInfo.size(); i++)
            {   
                DeviceInfo printDev = deviceInfo.get(i);                    // i ��° ����� ��ü�� ����̽� ���� ����
                System.out.println("["+i+"] Devcie info");                  
                System.out.println("MAC : " + printDev.getMacAddress());
                
                ArrayList<String> accuracy = printDev.getAccuracy();
                System.out.print("Accuracy : ");
                for (int j = 0; j < accuracy.size(); j++)
                {   
                    System.out.print(accuracy.get(j) + " ");                
                }
                System.out.println();
                System.out.println();
            }
            System.out.println();
            
            /*
             * ���� ���� �Ǵ��ϴ� �˰����� ��Ʈ
             * ����ȭ �ð��� 1������ üũ�� ��� Beacon �ֱ� 500ms �����Ǿ� �־ 1�� ���� 120���� �����Ͱ� ������ ��
             * ������ ���� Beacon�� ���ڰ� �þ ���� �����Ǵ� �ӵ��� ���ϱ޼������� ������ 
             * �׽�Ʈ������ ����Ʈ�� 10���� �˰������� �����ϵ��� �׽�Ʈ
             * 1. �̻�ġ �� ����  
             * 2. 10���� �յ� 10% �ڸ�
             * 3. ��� 
             */
            System.out.println("----------------\t Accuraccy ���� ���� \t----------------");
            for (int i = 0; i < deviceInfo.size(); i++)
            {
                DeviceInfo accurcy = deviceInfo.get(i);         // ����Ʈ�� ����� Beacon ��ü�� �ϳ��� �ҷ��´�
                /*********************************************************************************************************
                 * 
                 * 										������ �Ǵ� ����    
                 * 
                 ********************************************************************************************************* 
                 */
                if(accurcy.getAccuracy().size() > 6)            // �ش� Beacon ��ü�� Accuracy ����Ʈ ���� 10���� ������ �Ǵ��� ���� ��
                {
                    System.out.println("Beacon minor : " + accurcy.getMinor());
                    System.out.print(accurcy.getMacAddress() + " " + accurcy.getMinor() + " �� ����� accuracy �� : ");
                    for(int j = 0; j < accurcy.getAccuracy().size(); j++)
                    {   
                        System.out.print(accurcy.getAccuracy().get(j) + " ");                        
                        if (j >= 2)
                        {
                            //System.out.println("���ϴ� �� : " + accurcy.getAccuracy().get(j));
                            avgAccuracy += Double.parseDouble(accurcy.getAccuracy().get(j));
                            //System.out.println("Current Accuracy sum : " + avgAccuracy);
                            
                            if(accuracyCount == 8)
                            {
                                accuracyCount = 0;
                                break;
                            }
                            accuracyCount++;
                        }
                    }
                    System.out.println();
                    System.out.println(accurcy.getMacAddress() + " " + accurcy.getMinor() + "�� Accuracy sum : " + avgAccuracy);
                    System.out.println(accurcy.getMacAddress() + " " + accurcy.getMinor() + "�� Accuracy avg : " + avgAccuracy / 6);
                    
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy��/MM��/dd��/HH:mm");
                    Date dateTime = new Date();

                    String chidrenUpdateTime = dateFormat.format(dateTime);
                    // yyyy��/MM��/dd��/HH:mm�� ���·� ����� ��¥ ���
                    System.out.println(chidrenUpdateTime);  
                    
                    /*
                     *  ����, ���� �Ǵ�
                     *  ������ ��� Accuracy ���� 3.0 ���� 
                     *  ������ ��� Accuracy ���� 4.0 �̻�
                     */
                    if(avgAccuracy <= 3.0)
                    {
                    	// ����
                    	System.out.println();
                    	System.out.println("------------------------------------");
                    	System.out.println("---------------- ���� ----------------");
                    	System.out.println("------------------------------------");
                        try
                        {
                        	fbs.isRidding(true, accurcy.getMajor(), accurcy.getMinor());
                        }
                        catch (Exception e) {
							// TODO: handle exception
                        	System.out.print("Firebase connection error // up");
						}
                        
                        	
                        
                    	
                    }
                    else if(avgAccuracy >= 4.0)
                    {
                    	// ����
                    	System.out.println();
                    	System.out.println("------------------------------------");
                    	System.out.println("---------------- ���� ----------------");
                    	System.out.println("------------------------------------");
                    	try
                    	{
                    		fbs.isRidding(false, accurcy.getMajor(), accurcy.getMinor());
                    	}
                    	catch (Exception e) {
							// TODO: handle exception
                    		System.out.print("Firebase connection error // down");
						}
                    	
                    }
                    
                    /*
                     * Beacon ��ü�� ����ؼ� �ش� ���� ������ ���� �ʿ䰡 ���ٰ� �Ǵ� 
                     * ��µ� ���� ��ü�� �����ϴ� �ڵ带 ����
                     */
                    //accurcy.setRssi(avgAccuracy / 6);   	// ��� accuracy ���� �ش� ��ü�� ����                  
                    //accurcy.setTime(chidrenUpdateTime);	// ���� �ð��� ����
                    accurcy.clearAccuracy();            	// ����� ����Ͽ� ������ �Ǵ� ������ ���� ����ϸ� ���Ҹ� ��� ������
                    accuracyCount = 0;                  	// �ʱ�ȭ
                    avgAccuracy = 0;                    	// �� �ʱ�ȭ 
                    System.out.println();
                    System.out.println();
                }
                
            }
            System.out.println();
            System.out.println();
            System.out.println();
            
            
            /*
            try
            {
                Thread.sleep(1000);
            }
            catch (InterruptedException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            */
        }

    }

}