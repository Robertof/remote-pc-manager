/**
 * capture
 * a simple webcam image acquirer by Robertof
 * <robertof[dot]public{at}gmail.com>
 *
 * Licensed under CC-Attribution. Which means:
 * - do everything you want, but at least mention me
 *   as the original author
 * Coded with vim for my Raspberry Pi.
 * Requires boost-dev and opencv-dev, compile with:
 * g++ -lopencv_core -lopencv_highgui -lboost_program_options capture.cpp -o capture
 */

#include "opencv2/highgui/highgui.hpp"
#include "opencv2/core/core.hpp"
#include <boost/program_options.hpp>
#include <iostream>
#include <cstdio>

using namespace cv;
using namespace std;

// returns an integer directly for the main() function
int print_help (boost::program_options::options_description desc, bool incl_header, string prepend = "")
{
	cerr << ( incl_header ? "capture - a simple webcam image acquirer by Robertof\n" : "" ) << prepend << desc << endl;
	return ( prepend.empty() ? 0 : 1 );
}

bool matches_at_least (string allowed_values[], size_t size, string match_with)
{
	for (int j = 0; j < size; j++)
	{
		if (allowed_values[j].compare (match_with) == 0)
			return true;
	}
	return false;
}

int main (int argc, char* argv[])
{
	boost::program_options::options_description desc("Available parameters");
	string outputPath;
	string format;
	int qualityLevel;
	int compression;
	int device;
	int width;
	int height;
	bool quiet = false;
	desc.add_options()
		("help,H", "Displays this help message")
		("output,o", boost::program_options::value<string>(&outputPath), "Defines the desidered output path. It should be a png/jpg/ppm file, otherwise you should specify the type with --type")
		("quality", boost::program_options::value<int>(&qualityLevel)->default_value (95), "Defines the desidered quality. Works only with JPG, default is 95")
		("compression", boost::program_options::value<int>(&compression)->default_value (3), "Specifies the desidered compression level, between 0-9. Works only with PNG, default is 3")
		("device,d", boost::program_options::value<int>(&device)->default_value (-1), "Specifies the target V4L2 device, it should be a numeric integer (usually the one from /dev/videoX). Defaults to -1, aka 'the first available device'")
		("height,h", boost::program_options::value<int>(&height)->default_value (240), "Defines the height of the output image. Defaults to 240")
		("width,w", boost::program_options::value<int>(&width)->default_value (320), "Defines the width of the output image. Defaults to 320")
		("type,t", boost::program_options::value<string>(&format), "Specifies with which format the image will be saved. Available types: png, jpg, ppm")
		("quiet,q", "Suppresses the output. Note that OpenCV warnings cannot be suppressed")
	;
	boost::program_options::variables_map vmap;
	boost::program_options::store (boost::program_options::parse_command_line (argc, argv, desc), vmap);
	boost::program_options::notify (vmap);
	if (vmap.count ("help"))
		return print_help (desc, true);
	else if (vmap.count ("quiet"))
		quiet = true;
	if (!vmap.count ("output"))
		return print_help (desc, false, "You need to specify an output path with -o.\n");
	// parse the output extension and determine which imagetype should be used
	string extensions[] = { "png", "jpg", "ppm" };
	string current_ext = outputPath.substr (outputPath.length() - 3, 3);
	bool use_format = false;
	if (vmap.count ("type") && matches_at_least (extensions, 3, format))
	{
		if (format.compare (current_ext) != 0)
			use_format = true;
	}
	else if (outputPath.at (outputPath.length() - 4) != '.' || !matches_at_least (extensions, 3, current_ext))
	{
		return print_help (desc, false, "Invalid file extension. Try specifying one (valid) with '--type'.\n");
	}
	// sanitize quality/other values
	if (qualityLevel < 0 || qualityLevel > 100 ||
	    compression  < 0 || compression  >   9)
		return print_help (desc, false, "Invalid compression / quality levels. The first one should be >= 0 && <= 9, the second one >= 0 && <= 100");
	if (device < -1)
		return print_help (desc, false, "Invalid device. It should be >= -1.");
	// all done, time to capture da stuff
	if (!quiet)
		cout << "Capturing the image, please wait a moment..." << endl;
	try {
		VideoCapture v(device);
		v.set (CV_CAP_PROP_FRAME_WIDTH, width);
		v.set (CV_CAP_PROP_FRAME_HEIGHT, height);
		Mat targetImg;
		v >> targetImg;
		string sName = outputPath;
		string finalExt = current_ext;
		if (use_format)
		{
			sName += "." + format;
			finalExt = format;
		}
		// generate params
		bool isPng = finalExt.compare ("png") == 0;
		//cout << "dbg, sName " << sName << ", fExt " << finalExt << ", isPng " << isPng << endl;
		vector<int> params;
		if ( ( isPng && compression  != 3 ) ||
		     ( finalExt.compare ("jpg") == 0 && qualityLevel != 95 ))
		{
			params.push_back (isPng ? CV_IMWRITE_PNG_COMPRESSION : CV_IMWRITE_JPEG_QUALITY);
			params.push_back (isPng ? compression : qualityLevel);
		}
		imwrite (sName, targetImg, params);
		if (use_format)
			rename (sName.c_str(), outputPath.c_str());
		if (!quiet)
			cout << "Capture completed." << endl;
	}
	catch (std::exception& ex)
	{
		cerr << "Got an exception while capturing the image: " << ex.what() << endl;
		return 1;
	}
	return 0;
}
