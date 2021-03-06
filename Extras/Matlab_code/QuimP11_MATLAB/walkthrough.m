
% Ensure the current folder is set to QuimP11_MATLAB, or add
% the QuimP11_MATLAB to to [File->Set Path] so QuimP's functions
% are useable.


% Read in the data output by QuimP11.  Missing files will be skipped.
% When prompted, select the folder containing the associated paQP file.
qCells = readQanalysis();

% qCells is a structure. Each element is data for one analysis
% (i.e. one paQP file). If only one paQP file was located
% then qCells will be of length of 1.

c = qCells(1); % we will extract the first analysis into 'c'.

c  % type c and you will see a list of the data in the analysis

% to access data, for example the fluoMap, type 'c.fluoMap'

% load the image used for channel 1
% (if this fails, the path c.FLUOCH1TIFF is probably wrong due to it being moved)
info = imfinfo(c.FLUOCH1TIFF,'TIFF');  % structure array with data about each image in the stack
im = imread(c.FLUOCH1TIFF, 3, 'Info', info); % load frame 3
figure(1);
imagesc(im);


% plot the motility and fluo maps
figure(2);
plotMap(c.motilityMap, 'm', c.FI); % 'm' tells plotMap to format as a motility map.
                                    % c.FI is the frame interval for
                                    % scaling.
figure(3);
plotMap(c.fluoCh1Map, 'f', c.FI); % 'f' for fluorescence map


xFM = xcorrQ( c.motilityMap,c.fluoCh1Map ); % calculate a cross correlation
figure(4);
imagesc(xFM); title('Motility Map Xcorr with Fluo Map');

% Red is strong correlation, blue strong negative correlation.
% We can also do auto-correlation ( correlate maps with themselves) to
% search for repetitive patterns of motility/fluorescence.

xMM = xcorrQ( c.motilityMap, c.motilityMap); % calc an auto correlation
figure(5);
imagesc(xMM); title('AutoCorr of Motility Map');


% Plot the cell outlines, stored in c.outlines.
% This a cell array. Each cell as the outline for one frame.
% See c.outlineHeaders
% for the contents of the columns. We want columns 3 and 4.
c.outlineHeaders
% e.g
c.outlines{7}(:,3:4) % x and y co-ordinates at frame 7

figure(6);
colours = hot(c.nbFrames); % create a colour chart
hold off;
% plot all the cell outline
for i = 1:c.nbFrames,
    plotOutline(c.outlines{i}, colours(i,:));
    hold on;
end
hold off;
axis equal;
axis(c.R); % set the axis bounds
set(gca,'YDir','reverse'); % flip the Y-axis around so matlab plots (0,0) at the top left


% plot and colour and outline according to membrane speed
% The maxMigration value is used to scale the colours
figure(7);
scale = max(abs(c.maxSpeed)); % we take the max velocity across all frames and include negative values 
plotMotility(c.outlines{35}, scale, 5); % frame 35. red is expansion, blue contraction
axis(c.R); % set the axis bounds
axis equal;
set(gca,'YDir','reverse'); % flip the Y-axis around so matlab plots (0,0) at the top left
% this function needs a bit of work to improve its visuals

%-------------------------------------
% plot some global statistics

% c.stats is a matrix, rows are frames.
% see c.statHeaders for column contents

% path of the cell centroid
figure(8)
plot(c.stats(:,2), c.stats(:,3));
axis equal

% change in cell area
figure(9)
plot(c.stats(:,1), c.stats(:,11));
axis equal

% Fluorescence stats are slightly more complicated, as there re three
% channels, but is in the same format as c.stats
c.fluoStats % all the data
% the dimensions are: (Frame, Channel, measure),
% where measure one of those listed in c.fluostatsHeaders
c.fluoStatHeaders

% for example
c.fluoStats(:,2,7); % for all frame, channel 2, mean cyto fluorescence
c.fluoStats(1:10,1,11); % for the first 10 frames, channel 1, %age cortex fluo.

%plot them against frame number
figure(10);
plot(c.frames, c.fluoStats(:,1,7),c.frames, c.fluoStats(:,1,11));
%------------------------------------------

%---------------------------
% tracking through maps
% We can track using ECMM data recorded in the co-ordinate and origin maps
% I have provided all the code to do so

% track forwards. Typically, the backward tracking gives better results,
% due to the nature of ECMM mapping.
figure(11);
hold off
plotMap(c.motilityMap, 'm', c.FI); % plot the motility map

trackF = trackForward( c.forwardMap, 1 , 120, 35 );
hold on
plot(trackF(:,2), trackF(:,1), 'r');

% track backwards

trackB = trackBackward( c.backwardMap, 30 , 223, 30 );
hold on
plot(trackB(:,2), trackB(:,1), 'b');