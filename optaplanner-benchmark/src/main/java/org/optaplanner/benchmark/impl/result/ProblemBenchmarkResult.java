/*
 * Copyright 2011 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.optaplanner.benchmark.impl.result;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;
import com.thoughtworks.xstream.annotations.XStreamOmitField;
import org.apache.commons.lang3.StringUtils;
import org.optaplanner.benchmark.config.ProblemBenchmarksConfig;
import org.optaplanner.benchmark.config.statistic.ProblemStatisticType;
import org.optaplanner.benchmark.config.statistic.SingleStatisticType;
import org.optaplanner.benchmark.impl.measurement.ScoreDifferencePercentage;
import org.optaplanner.benchmark.impl.ranking.SingleBenchmarkRankingComparator;
import org.optaplanner.benchmark.impl.report.BenchmarkReport;
import org.optaplanner.benchmark.impl.report.ReportHelper;
import org.optaplanner.benchmark.impl.statistic.ProblemStatistic;
import org.optaplanner.benchmark.impl.statistic.PureSubSingleStatistic;
import org.optaplanner.core.api.solver.Solver;
import org.optaplanner.core.config.SolverConfigContext;
import org.optaplanner.core.config.util.ConfigUtils;
import org.optaplanner.persistence.common.api.domain.solution.SolutionFileIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents 1 problem instance (data set) benchmarked on multiple {@link Solver} configurations.
 */
@XStreamAlias("problemBenchmarkResult")
public class ProblemBenchmarkResult<Solution_> {

    protected final transient Logger logger = LoggerFactory.getLogger(getClass());

    @XStreamOmitField // Bi-directional relationship restored through BenchmarkResultIO
    private PlannerBenchmarkResult plannerBenchmarkResult;

    private String name = null;

    @XStreamOmitField // TODO move solutionFileIO out of ProblemBenchmarkResult
    private SolutionFileIO<Solution_> solutionFileIO = null;
    private boolean writeOutputSolutionEnabled = false;

    private File inputSolutionFile = null;

    @XStreamImplicit(itemFieldName = "problemStatistic")
    private List<ProblemStatistic> problemStatisticList = null;

    @XStreamImplicit(itemFieldName = "singleBenchmarkResult")
    private List<SingleBenchmarkResult> singleBenchmarkResultList = null;

    private Long entityCount = null;
    private Long variableCount = null;
    private Long maximumValueCount = null;
    private Long problemScale = null;

    @XStreamOmitField // Loaded lazily from singleBenchmarkResults
    private Integer maximumSubSingleCount = null;

    // ************************************************************************
    // Report accumulates
    // ************************************************************************

    private Long averageUsedMemoryAfterInputSolution = null;
    private Integer failureCount = null;
    private SingleBenchmarkResult winningSingleBenchmarkResult = null;
    private SingleBenchmarkResult worstSingleBenchmarkResult = null;

    // ************************************************************************
    // Constructors and simple getters/setters
    // ************************************************************************

    public ProblemBenchmarkResult(PlannerBenchmarkResult plannerBenchmarkResult) {
        this.plannerBenchmarkResult = plannerBenchmarkResult;
    }

    public PlannerBenchmarkResult getPlannerBenchmarkResult() {
        return plannerBenchmarkResult;
    }

    public void setPlannerBenchmarkResult(PlannerBenchmarkResult plannerBenchmarkResult) {
        this.plannerBenchmarkResult = plannerBenchmarkResult;
    }

    /**
     * @return never null, filename safe
     */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public SolutionFileIO<Solution_> getSolutionFileIO() {
        return solutionFileIO;
    }

    public void setSolutionFileIO(SolutionFileIO<Solution_> solutionFileIO) {
        this.solutionFileIO = solutionFileIO;
    }

    public boolean isWriteOutputSolutionEnabled() {
        return writeOutputSolutionEnabled;
    }

    public void setWriteOutputSolutionEnabled(boolean writeOutputSolutionEnabled) {
        this.writeOutputSolutionEnabled = writeOutputSolutionEnabled;
    }

    public File getInputSolutionFile() {
        return inputSolutionFile;
    }

    public void setInputSolutionFile(File inputSolutionFile) {
        this.inputSolutionFile = inputSolutionFile;
    }

    public List<ProblemStatistic> getProblemStatisticList() {
        return problemStatisticList;
    }

    public void setProblemStatisticList(List<ProblemStatistic> problemStatisticList) {
        this.problemStatisticList = problemStatisticList;
    }

    public List<SingleBenchmarkResult> getSingleBenchmarkResultList() {
        return singleBenchmarkResultList;
    }

    public void setSingleBenchmarkResultList(List<SingleBenchmarkResult> singleBenchmarkResultList) {
        this.singleBenchmarkResultList = singleBenchmarkResultList;
    }

    public Long getEntityCount() {
        return entityCount;
    }

    public Long getVariableCount() {
        return variableCount;
    }

    public Long getMaximumValueCount() {
        return maximumValueCount;
    }

    public Long getProblemScale() {
        return problemScale;
    }

    public Integer getMaximumSubSingleCount() {
        return maximumSubSingleCount;
    }

    public Long getAverageUsedMemoryAfterInputSolution() {
        return averageUsedMemoryAfterInputSolution;
    }

    public Integer getFailureCount() {
        return failureCount;
    }

    public SingleBenchmarkResult getWinningSingleBenchmarkResult() {
        return winningSingleBenchmarkResult;
    }

    public SingleBenchmarkResult getWorstSingleBenchmarkResult() {
        return worstSingleBenchmarkResult;
    }

    // ************************************************************************
    // Smart getters
    // ************************************************************************

    public String getAnchorId() {
        return ReportHelper.escapeHtmlId(name);
    }

    public String findScoreLevelLabel(int scoreLevel) {
        String[] levelLabels = singleBenchmarkResultList.get(0).getSolverBenchmarkResult().getScoreDefinition().getLevelLabels();
        return levelLabels[scoreLevel];
    }

    public File getBenchmarkReportDirectory() {
        return plannerBenchmarkResult.getBenchmarkReportDirectory();
    }

    public boolean hasAnyFailure() {
        return failureCount > 0;
    }

    public boolean hasAnySuccess() {
        return singleBenchmarkResultList.size() - failureCount > 0;
    }

    public boolean hasAnyStatistic() {
        if (problemStatisticList.size() > 0) {
            return true;
        }
        for (SingleBenchmarkResult singleBenchmarkResult : singleBenchmarkResultList) {
            if (singleBenchmarkResult.getMedian().getPureSubSingleStatisticList().size() > 0) {
                return true;
            }
        }
        return false;
    }

    public boolean hasProblemStatisticType(ProblemStatisticType problemStatisticType) {
        for (ProblemStatistic problemStatistic : problemStatisticList) {
            if (problemStatistic.getProblemStatisticType() == problemStatisticType) {
                return true;
            }
        }
        return false;
    }

    public Collection<SingleStatisticType> extractSingleStatisticTypeList() {
        Set<SingleStatisticType> singleStatisticTypeSet = new LinkedHashSet<>();
        for (SingleBenchmarkResult singleBenchmarkResult : singleBenchmarkResultList) {
            for (PureSubSingleStatistic pureSubSingleStatistic : singleBenchmarkResult.getMedian().getPureSubSingleStatisticList()) {
                singleStatisticTypeSet.add(pureSubSingleStatistic.getStatisticType());
            }
        }
        return singleStatisticTypeSet;
    }

    public List<PureSubSingleStatistic> extractPureSubSingleStatisticList(SingleStatisticType singleStatisticType) {
        List<PureSubSingleStatistic> pureSubSingleStatisticList = new ArrayList<>(
                singleBenchmarkResultList.size());
        for (SingleBenchmarkResult singleBenchmarkResult : singleBenchmarkResultList) {
            for (PureSubSingleStatistic pureSubSingleStatistic : singleBenchmarkResult.getMedian().getPureSubSingleStatisticList()) {
                if (pureSubSingleStatistic.getStatisticType() == singleStatisticType) {
                    pureSubSingleStatisticList.add(pureSubSingleStatistic);
                }
            }
        }
        return pureSubSingleStatisticList;
    }

    // ************************************************************************
    // Work methods
    // ************************************************************************

    public String getProblemReportDirectoryPath() {
        return name;
    }

    public File getProblemReportDirectory() {
        return new File(getBenchmarkReportDirectory(), name);
    }

    public void makeDirs() {
        File problemReportDirectory = getProblemReportDirectory();
        problemReportDirectory.mkdirs();
        for (SingleBenchmarkResult singleBenchmarkResult : singleBenchmarkResultList) {
            singleBenchmarkResult.makeDirs();
        }
    }

    public int getTotalSubSingleCount() {
        int totalSubSingleCount = 0;
        for (SingleBenchmarkResult singleBenchmarkResult : singleBenchmarkResultList) {
            totalSubSingleCount += singleBenchmarkResult.getSubSingleCount();
        }
        return totalSubSingleCount;
    }

    public Solution_ readPlanningProblem() {
        return solutionFileIO.read(inputSolutionFile);
    }

    public void writeOutputSolution(SubSingleBenchmarkResult subSingleBenchmarkResult, Solution_ outputSolution) {
        if (!writeOutputSolutionEnabled) {
            return;
        }
        String filename = getName() + "." + solutionFileIO.getOutputFileExtension();
        File outputSolutionFile = new File(subSingleBenchmarkResult.getResultDirectory(), filename);
        solutionFileIO.write(outputSolution, outputSolutionFile);
    }

    // ************************************************************************
    // Accumulate methods
    // ************************************************************************

    public void accumulateResults(BenchmarkReport benchmarkReport) {
        for (SingleBenchmarkResult singleBenchmarkResult : singleBenchmarkResultList) {
            singleBenchmarkResult.accumulateResults(benchmarkReport);
        }
        determineTotalsAndAveragesAndRanking();
        determineWinningScoreDifference();
        for (ProblemStatistic problemStatistic : problemStatisticList) {
            problemStatistic.accumulateResults(benchmarkReport);
        }
    }

    private void determineTotalsAndAveragesAndRanking() {
        failureCount = 0;
        maximumSubSingleCount = 0;
        long totalUsedMemoryAfterInputSolution = 0L;
        int usedMemoryAfterInputSolutionCount = 0;
        List<SingleBenchmarkResult> successResultList = new ArrayList<>(singleBenchmarkResultList);
        // Do not rank a SingleBenchmarkResult that has a failure
        for (Iterator<SingleBenchmarkResult> it = successResultList.iterator(); it.hasNext(); ) {
            SingleBenchmarkResult singleBenchmarkResult = it.next();
            if (singleBenchmarkResult.hasAnyFailure()) {
                failureCount++;
                it.remove();
            } else {
                int subSingleCount = singleBenchmarkResult.getSubSingleBenchmarkResultList().size();
                if (subSingleCount > maximumSubSingleCount) {
                    maximumSubSingleCount = subSingleCount;
                }
                if (singleBenchmarkResult.getUsedMemoryAfterInputSolution() != null) {
                    totalUsedMemoryAfterInputSolution += singleBenchmarkResult.getUsedMemoryAfterInputSolution();
                    usedMemoryAfterInputSolutionCount++;
                }
            }
        }
        if (usedMemoryAfterInputSolutionCount > 0) {
            averageUsedMemoryAfterInputSolution = totalUsedMemoryAfterInputSolution
                    / (long) usedMemoryAfterInputSolutionCount;
        }
        determineRanking(successResultList);
    }

    private void determineRanking(List<SingleBenchmarkResult> rankedSingleBenchmarkResultList) {
        Comparator singleBenchmarkRankingComparator = new SingleBenchmarkRankingComparator();
        Collections.sort(rankedSingleBenchmarkResultList, Collections.reverseOrder(singleBenchmarkRankingComparator));
        int ranking = 0;
        SingleBenchmarkResult previousSingleBenchmarkResult = null;
        int previousSameRankingCount = 0;
        for (SingleBenchmarkResult singleBenchmarkResult : rankedSingleBenchmarkResultList) {
            if (previousSingleBenchmarkResult != null
                    && singleBenchmarkRankingComparator.compare(previousSingleBenchmarkResult, singleBenchmarkResult) != 0) {
                ranking += previousSameRankingCount;
                previousSameRankingCount = 0;
            }
            singleBenchmarkResult.setRanking(ranking);
            previousSingleBenchmarkResult = singleBenchmarkResult;
            previousSameRankingCount++;
        }
        winningSingleBenchmarkResult = rankedSingleBenchmarkResultList.isEmpty() ? null : rankedSingleBenchmarkResultList.get(0);
        worstSingleBenchmarkResult = rankedSingleBenchmarkResultList.isEmpty() ? null
                : rankedSingleBenchmarkResultList.get(rankedSingleBenchmarkResultList.size() - 1);
    }

    private void determineWinningScoreDifference() {
        for (SingleBenchmarkResult singleBenchmarkResult : singleBenchmarkResultList) {
            if (singleBenchmarkResult.hasAnyFailure()) {
                continue;
            }
            singleBenchmarkResult.setWinningScoreDifference(
                    singleBenchmarkResult.getAverageScore().subtract(winningSingleBenchmarkResult.getAverageScore()));
            singleBenchmarkResult.setWorstScoreDifferencePercentage(
                    ScoreDifferencePercentage.calculateScoreDifferencePercentage(
                            worstSingleBenchmarkResult.getAverageScore(), singleBenchmarkResult.getAverageScore()));
        }
    }

    /**
     * HACK to avoid loading the planningProblem just to extract it's problemScale.
     * Called multiple times, for every {@link SingleBenchmarkResult} of this {@link ProblemBenchmarkResult}.
     *
     * @param registeringEntityCount {@code >= 0}
     * @param registeringVariableCount {@code >= 0}
     * @param registeringProblemScale {@code >= 0}
     */
    public void registerScale(long registeringEntityCount, long registeringVariableCount,
            long registeringMaximumValueCount, long registeringProblemScale) {
        if (entityCount == null) {
            entityCount = registeringEntityCount;
        } else if (entityCount.longValue() != registeringEntityCount) {
            logger.warn("The problemBenchmarkResult ({}) has different entityCount values ([{},{}]).\n"
                    + "This is normally impossible for 1 inputSolutionFile.",
                    getName(), entityCount, registeringEntityCount);
            // The entityCount is not unknown (null), but known to be ambiguous
            entityCount = -1L;
        }
        if (variableCount == null) {
            variableCount = registeringVariableCount;
        } else if (variableCount.longValue() != registeringVariableCount) {
            logger.warn("The problemBenchmarkResult ({}) has different variableCount values ([{},{}]).\n"
                    + "This is normally impossible for 1 inputSolutionFile.",
                    getName(), variableCount, registeringVariableCount);
            // The variableCount is not unknown (null), but known to be ambiguous
            variableCount = -1L;
        }
        if (maximumValueCount == null) {
            maximumValueCount = registeringMaximumValueCount;
        } else if (maximumValueCount.longValue() != registeringMaximumValueCount) {
            logger.warn("The problemBenchmarkResult ({}) has different maximumValueCount values ([{},{}]).\n"
                    + "This is normally impossible for 1 inputSolutionFile.",
                    getName(), maximumValueCount, registeringMaximumValueCount);
            // The maximumValueCount is not unknown (null), but known to be ambiguous
            maximumValueCount = -1L;
        }
        if (problemScale == null) {
            problemScale = registeringProblemScale;
        } else if (problemScale.longValue() != registeringProblemScale) {
            logger.warn("The problemBenchmarkResult ({}) has different problemScale values ([{},{}]).\n"
                    + "This is normally impossible for 1 inputSolutionFile.",
                    getName(), problemScale, registeringProblemScale);
            // The problemScale is not unknown (null), but known to be ambiguous
            problemScale = -1L;
        }
    }

    /**
     * Used by {@link ProblemBenchmarksConfig#buildProblemBenchmarkList(SolverConfigContext, SolverBenchmarkResult)}.
     * @param o sometimes null
     * @return true if equal
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o instanceof ProblemBenchmarkResult) {
            ProblemBenchmarkResult other = (ProblemBenchmarkResult) o;
            return inputSolutionFile.equals(other.getInputSolutionFile());
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return inputSolutionFile.hashCode();
    }

    // ************************************************************************
    // Merger methods
    // ************************************************************************

    protected static <Solution_> Map<ProblemBenchmarkResult, ProblemBenchmarkResult> createMergeMap(
            PlannerBenchmarkResult newPlannerBenchmarkResult, List<SingleBenchmarkResult> singleBenchmarkResultList) {
        // IdentityHashMap but despite that different ProblemBenchmarkResult instances are merged
        Map<ProblemBenchmarkResult, ProblemBenchmarkResult> mergeMap
                = new IdentityHashMap<>();
        Map<File, ProblemBenchmarkResult> fileToNewResultMap = new HashMap<>();
        for (SingleBenchmarkResult singleBenchmarkResult : singleBenchmarkResultList) {
            ProblemBenchmarkResult<Solution_> oldResult = singleBenchmarkResult.getProblemBenchmarkResult();
            if (!mergeMap.containsKey(oldResult)) {
                ProblemBenchmarkResult<Solution_> newResult;
                if (!fileToNewResultMap.containsKey(oldResult.inputSolutionFile)) {
                    newResult = new ProblemBenchmarkResult<>(newPlannerBenchmarkResult);
                    newResult.name = oldResult.name;
                    newResult.inputSolutionFile = oldResult.inputSolutionFile;
                    // Skip oldResult.problemReportDirectory
                    newResult.problemStatisticList = new ArrayList<>(oldResult.problemStatisticList.size());
                    for (ProblemStatistic oldProblemStatistic : oldResult.problemStatisticList) {
                        newResult.problemStatisticList.add(
                                oldProblemStatistic.getProblemStatisticType().buildProblemStatistic(newResult));
                    }
                    newResult.singleBenchmarkResultList = new ArrayList<>(
                            oldResult.singleBenchmarkResultList.size());
                    newResult.entityCount = oldResult.entityCount;
                    newResult.variableCount = oldResult.variableCount;
                    newResult.maximumValueCount = oldResult.maximumValueCount;
                    newResult.problemScale = oldResult.problemScale;
                    fileToNewResultMap.put(oldResult.inputSolutionFile, newResult);
                    newPlannerBenchmarkResult.getUnifiedProblemBenchmarkResultList().add(newResult);
                } else {
                    newResult = fileToNewResultMap.get(oldResult.inputSolutionFile);
                    if (!Objects.equals(oldResult.name, newResult.name)) {
                        throw new IllegalStateException(
                                "The oldResult (" + oldResult + ") and newResult (" + newResult
                                + ") should have the same name, because they have the same inputSolutionFile ("
                                + oldResult.inputSolutionFile + ").");
                    }
                    for (Iterator<ProblemStatistic> it = newResult.problemStatisticList.iterator(); it.hasNext(); ) {
                        ProblemStatistic newStatistic = it.next();
                        if (!oldResult.hasProblemStatisticType(newStatistic.getProblemStatisticType())) {
                            it.remove();
                        }
                    }
                    newResult.entityCount = ConfigUtils.meldProperty(oldResult.entityCount, newResult.entityCount);
                    newResult.variableCount = ConfigUtils.meldProperty(oldResult.variableCount, newResult.variableCount);
                    newResult.maximumValueCount = ConfigUtils.meldProperty(oldResult.maximumValueCount, newResult.maximumValueCount);
                    newResult.problemScale = ConfigUtils.meldProperty(oldResult.problemScale, newResult.problemScale);
                }
                mergeMap.put(oldResult, newResult);
            }
        }
        return mergeMap;
    }

    @Override
    public String toString() {
        return getName();
    }

}
