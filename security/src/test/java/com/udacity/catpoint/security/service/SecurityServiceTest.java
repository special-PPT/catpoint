package com.udacity.catpoint.security.service;

import com.udacity.catpoint.image.service.ImageService;
import com.udacity.catpoint.security.application.StatusListener;
import com.udacity.catpoint.security.data.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SecurityServiceTest {

    private SecurityService securityService;
    private Sensor sensor;

    @Mock
    private Set<StatusListener> statusListeners;

    @Mock
    private ImageService imageService;

    @Mock
    private SecurityRepository securityRepository;

    private Sensor setASensor(String name) {
        return new Sensor(name, SensorType.WINDOW);
    }
    private Set<Sensor> setAllSensors(int num, boolean statue) {
        Set<Sensor> sensors = new HashSet<>();
        for(int i = 0; i < num; i++) {
            sensors.add(setASensor(String.valueOf(i)));
        }
        sensors.forEach(sensor -> sensor.setActive(statue));
        return sensors;
    }
    @BeforeEach
    void init() {
        this.securityService = new SecurityService(securityRepository, imageService);
        sensor = new Sensor("sensor1", SensorType.DOOR);
    }
 // 1 requirement
    @Test
    public void ifAlarmIsArmedAndSensorIsActivated_SystemIsInPendingAlarmStatus() {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        securityService.changeSensorActivationStatus(sensor, Boolean.TRUE);
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }
    // 2 requirement
    @Test
    public void ifAlarmIsArmedAndSensorBecomesActivated_SystemIsPendingAlarm_AlarmStatusIsInAlarm() {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        securityService.changeSensorActivationStatus(sensor, Boolean.TRUE);
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    // 3 requirement seems fail
    @Test
    public void ifPendingAlarm_SensorsAreInactive_ReturnToNoAlarmState() {
        when(securityRepository.getAlarmStatus()).thenReturn((AlarmStatus.PENDING_ALARM));
        sensor.setActive(false);
        securityService.changeSensorActivationStatus(sensor, false);
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);

    }

    // 4 requirement
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void ifAlarmIsActive_ChangeSensorState_NotAffectOnAlarmState(boolean statue) {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        securityService.changeSensorActivationStatus(sensor, statue);
        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

    // 5 requirement
    @Test
    public void ifSensorIsActivedWhileAlreadyActive_SystemIsInPendingState_ChangeToAlarmState() {
        sensor.setActive(true);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.changeSensorActivationStatus(sensor, true);
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    // 6 requirement
    @Test
    public void ifSensorIsDeactivatedWhileAlreadyInactive_MakeNoChangesToTheAlarmState() {
        sensor.setActive(false);
        securityService.changeSensorActivationStatus(sensor, false);
        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

    // 7 requirement
    @Test
    public void ifTheImageServiceIdentifiesAnImageContainingACat_WhileTheSystemIsArmHome_PutTheSystemIntoAlarmStatus() {
        BufferedImage bufferedImage = new BufferedImage(200,200,BufferedImage.TYPE_INT_RGB);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(true);
        securityService.processImage(bufferedImage);
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    // 8 requirement
    @Test
    public void ifTheImageServiceIdentifiedAnImageThatDoesNotContainACat_SensorAreNotActive_ChangeTheStatueToNoAlarm() {
        BufferedImage bufferedImage = new BufferedImage(200,200,BufferedImage.TYPE_INT_RGB);
        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(false);
        sensor.setActive(false);
        securityService.processImage(bufferedImage);
        verify(securityRepository, times(1)).setAlarmStatus((AlarmStatus.NO_ALARM));
    }

    // 9 requirement
    @Test
    public void ifTheSystemIsDisarmed_SetTheStatusToNoAlarm() {
        securityService.setArmingStatus(ArmingStatus.DISARMED);
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    // 10 requirement
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class,names = {"ARMED_HOME", "ARMED_AWAY"})
    public void ifTheSystemIsArmed_ResetAllSensorToInactive(ArmingStatus statue) {
        Set<Sensor> sensors = setAllSensors(3, true);
        when(securityRepository.getSensors()).thenReturn(sensors);
        securityService.setArmingStatus(statue);
        securityService.getSensors().forEach(sensor1 -> assertFalse(sensor1.getActive()));

    }

    // 11 requirement
    @Test
    public void ifTheSystemIsArmedHome_CameraShowsACat_SetTheAlarmStatusToAlarm() {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        BufferedImage bufferedImage = new BufferedImage(200,200,BufferedImage.TYPE_INT_RGB);
        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(true);
        securityService.processImage(bufferedImage);
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }


    // additional test to make a more complete coverage
    @Test
    public void ifSensorIsDeactivatedWhileActive_TheSystemIsInPendStatue_ChangeTheStatueToNoAlarmStatue() {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        sensor.setActive(true);
        securityService.changeSensorActivationStatus(sensor, false);
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @Test
    public void ifSensorIsActivatedWhileInactive_TheSystemIsInDisarmStatue_NoChangeToAlarmStatue() {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);
        sensor.setActive(false);
        securityService.changeSensorActivationStatus(sensor, true);
        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }
}
