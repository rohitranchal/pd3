var debug = require('debug')('shopping');
var express = require('express');
var router = express.Router();
var request = require('request');
var spawn = require('child_process').spawn;
var async = require('async');
var portscanner = require('portscanner');
var ab_client = require('../ab_client');
//var db = require('../db');
var fs = require('fs');

var ab_host = '127.0.0.1';
var seller_ip = '127.0.0.1'
var payment_ip = '127.0.0.1'
var ab_path = 'resources/AB8.jar';
var ab_lib = 'resources/lib';
var ab_arg = ab_path + ':./' + ab_lib + '/*:.';
var ab_class = 'edu.purdue.absoa.Server';
var req_data = ['ab.user.name', 'ab.user.payment.type'];
//var req_data = 'ab.user.name'

/* GET home page. */
router.get('/', function(req, res) {
	res.send('Shopping Service');
});

/*
router.get('/order', function(req, res) {

	request('http://localhost:4102/submit_order', function (error, response, body1) {
		var msg;
		if (body1.search('failed') != -1) {
			msg = 'Order failed - ' + body1;
			res.send(msg);
			var obj = {id:1, log:msg};
			db.set_service_log(obj, function() {});
		} else {
			request('http://localhost:4104/pay', function (error, response, body2) {
				if (body2.search('failed') != -1) {
					msg = 'Order failed - ' + body2;
					res.send(msg);
				} else {
					msg = 'Order complete details - ' + body1;
					res.send(msg);
				}
				var obj = {id:1, log:msg};
				db.set_service_log(obj, function() {});
			});
		}
	});
	// parse ab
	// start ab
	// get data
	// diplay data on console
	// send request to seller
});
*/

router.get('/ab_order', function(req, res) {
	var ab_data = null;
	var msg;

	var buf = fs.readFileSync(ab_path);
	var abfileStr = buf.toString('base64');


	var formData = {
	  my_file: fs.createReadStream(ab_path)
	}


	async.parallel([
		function(callback) {
			start_ab(ab_path, function(ab_port, ab_pid) {
				connect_ab(ab_port, ab_host, ab_pid, function(data) {
					ab_data = data;
					callback(null);
				});
			});			
		},
		function(callback) {
			request.post({url:'http://'+seller_ip+':4102/ab_submit_order',formData: formData}, function (error, response, body1) {
				if (body1.search('failed') != -1) {
					msg = 'Order failed - ' + body1;
					callback(null);
				} else {
					var newFormData = {
	  					my_file: fs.createReadStream(ab_path)
					}
					request.post({url:'http://'+payment_ip+':4104/ab_pay',formData: newFormData}, function (error, response, body2) {
						if (body2.search('failed') != -1) {
							msg = 'Order failed - ' + body2;
						} else {
							msg = 'Order complete details - ' + body1;
							console.log(msg)
						}
						callback(null);
					});
				}
			})
		}
	],
	function(err, results) {
		if (ab_data == null) {
			msg = 'Shopping Failed: AB data unavailable';
			res.send(msg);
		} else {
			res.send(msg);
		}
		var obj = {id:1, log:msg};
		//db.set_service_log(obj, function() {});
	});
});

router.get('/test', function(req, res) {
	start_ab(ab_path, function(ab_port, ab_pid) {
		connect_ab(ab_port, ab_host, ab_pid, function(data) {
			ab_data = data;
		});
	});
	// var ab_port = 5555;
	// var ab_pid = 1111111110;
	// connect_ab(ab_port, ab_host, ab_pid);
	res.send('ok');
});


var start_ab = function(ab_path, cb) {
	var ab_port = randomIntInc(10000, 65000)
	//var child =	spawn('java',['-jar', ab_path, ab_port]);
	var child = spawn('java', ['-cp', ab_arg, ab_class, ab_port]);	
	console.log('LOG: Starting AB on Port: ' + ab_port);
	var ab_pid = child.pid;

	child.stdout.setEncoding('ASCII');
	child.stdout.on('data', function (data) {
		console.log('LOG(AB): ');
		console.log(data);
	});
	child.stderr.on('data', function (data) {
		console.log('ERROR(AB): ');
		// console.log(data);
		console.log(new Buffer(data, 'base64'));
	});
	child.on('close', function (code, signal) {
		console.log('LOG: Terminated AB');
	});
	cb(ab_port, ab_pid);
};

var connect_ab = function(port, host, pid, cb) {
	var ab_start_status = 0;
	async.whilst(
		function () { return ab_start_status == 0; },
		function (callback) {
			// Check if AB is running every 10 ms
			portscanner.checkPortStatus(port, host, function(error, status) {
				if(status == 'open'){
					ab_start_status = 1;
				}
				setTimeout(callback, 10);
			});
		},
		function (err) {
			// AB is running, query the AB for data
			ret_data = []
			ab_client.get_data(req_data[0], port, function(ab_data) {
				console.log('abdata: ' + ab_data);
				ret_data.push(ab_data)
				ab_client.get_data(req_data[1], port, function(ab_data) {
					console.log('abdata: ' + ab_data);
					process.kill(pid);
					ret_data.push(ab_data)
					cb(ab_data);
				});
			});
	});
};

function randomIntInc(low, high) {
	return Math.floor(Math.random() * (high - low + 1) + low);
}

module.exports = router;
