// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package com.team1701.robot;

import java.util.Optional;

import com.team1701.lib.commands.CommandLogger;
import com.team1701.robot.Configuration.Mode;
import edu.wpi.first.hal.AllianceStationID;
import edu.wpi.first.wpilibj.simulation.DriverStationSim;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.CommandScheduler;
import org.littletonrobotics.junction.LogFileUtil;
import org.littletonrobotics.junction.LoggedRobot;
import org.littletonrobotics.junction.Logger;
import org.littletonrobotics.junction.networktables.NT4Publisher;
import org.littletonrobotics.junction.wpilog.WPILOGReader;
import org.littletonrobotics.junction.wpilog.WPILOGWriter;

public class Robot extends LoggedRobot {
    private RobotContainer mRobotContainer;
    private Optional<Command> mAutonomousCommand = Optional.empty();

    @Override
    public void robotInit() {
        initializeAdvantageKit();
        mRobotContainer = new RobotContainer();
    }

    private void initializeAdvantageKit() {
        // Record metadata
        Logger.recordMetadata("RuntimeType", getRuntimeType().toString());
        Logger.recordMetadata("ProjectName", BuildConstants.MAVEN_NAME);
        Logger.recordMetadata("BuildDate", BuildConstants.BUILD_DATE);
        Logger.recordMetadata("GitSHA", BuildConstants.GIT_SHA);
        Logger.recordMetadata("GitDate", BuildConstants.GIT_DATE);
        Logger.recordMetadata("GitBranch", BuildConstants.GIT_BRANCH);
        switch (BuildConstants.DIRTY) {
            case 0:
                Logger.recordMetadata("GitDirty", "All changes committed");
                break;
            case 1:
                Logger.recordMetadata("GitDirty", "Uncomitted changes");
                break;
            default:
                Logger.recordMetadata("GitDirty", "Unknown");
                break;
        }

        // Set up data receivers & replay source
        switch (Configuration.getMode()) {
            case REAL:
                Logger.addDataReceiver(new WPILOGWriter("/media/sda1/"));
                Logger.addDataReceiver(new NT4Publisher());
                break;
            case SIMULATION:
                Logger.addDataReceiver(new NT4Publisher());
                break;
            case REPLAY:
                var logPath = LogFileUtil.findReplayLog();
                Logger.setReplaySource(new WPILOGReader(logPath));
                Logger.addDataReceiver(new WPILOGWriter(LogFileUtil.addPathSuffix(logPath, "_sim")));
                break;
        }

        // Start AdvantageKit logger
        setUseTiming(Configuration.getMode() != Mode.REPLAY);
        Logger.start();

        // Default to blue alliance in sim
        if (Configuration.getMode() == Mode.SIMULATION) {
            DriverStationSim.setAllianceStationId(AllianceStationID.Blue1);
        }
    }

    @Override
    public void robotPeriodic() {
        CommandScheduler.getInstance().run();
        CommandLogger.getInstance().periodic();
        SmartDashboard.putData(CommandScheduler.getInstance());
    }

    @Override
    public void autonomousInit() {
        CommandScheduler.getInstance().cancelAll();
        mAutonomousCommand = mRobotContainer.getAutonomousCommand();
        mAutonomousCommand.ifPresent(command -> CommandScheduler.getInstance().schedule(command));
    }

    @Override
    public void autonomousPeriodic() {}

    @Override
    public void teleopInit() {
        mAutonomousCommand.ifPresent(Command::cancel);
    }

    @Override
    public void teleopPeriodic() {}

    @Override
    public void disabledInit() {}

    @Override
    public void disabledPeriodic() {}

    @Override
    public void testInit() {
        CommandScheduler.getInstance().cancelAll();
    }

    @Override
    public void testPeriodic() {}

    @Override
    public void simulationInit() {}

    @Override
    public void simulationPeriodic() {}
}
