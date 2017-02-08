import {Injectable} from '@angular/core';
import {Headers, Http, RequestOptions} from '@angular/http';

import {Constants} from "../constants";
import {AbstractService} from "./abtract.service";
import {Observable} from 'rxjs/Observable';


@Injectable()
export class FileService extends AbstractService {
    private PATH_EXISTS_URL = "/file/pathExists";

    constructor (private _http: Http) {
        super();
    }

    pathExists(path: string): Observable<boolean> {
        let headers = new Headers();
        let options = new RequestOptions({ headers: headers });
        headers.append('Content-Type', 'application/json');
        headers.append('Accept', 'application/json');

        return this._http.post(Constants.REST_BASE + this.PATH_EXISTS_URL, path, options)
            .map(res => <boolean> res.json())
            .catch(this.handleError);
    }

    queryServerPathTargetType(path: string): Observable<PathTargetType> {
        return this._http.post(Constants.REST_BASE + "/file/pathTargetType", path, this.JSON_OPTIONS)
            .map(res => <PathTargetType> res.json())
            .catch(this.handleError);
    }
}

export type PathTargetType = "FILE" | "DIRECTORY" | null;
