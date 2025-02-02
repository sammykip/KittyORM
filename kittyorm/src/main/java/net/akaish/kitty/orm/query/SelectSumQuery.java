
/*
 * ---
 *
 *  Copyright (c) 2018 Denis Bogomolov (akaish)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * This file is a part of KittyORM project (KittyORM library), more information at
 * https://akaish.github.io/KittyORMPages/license/
 *
 * ---
 */

package net.akaish.kitty.orm.query;

import java.text.MessageFormat;

/**
 * Created by akaish on 08.04.18.
 * @author akaish (Denis Bogomolov)
 */

public class SelectSumQuery extends BaseKittyQuery {

	static final String SELECT_FROM = "SELECT SUM({0}) FROM {1}";
	private String sumColumn;

	public SelectSumQuery(String tableName) {
		super(tableName);
	}

	public void setSumColumn(String sumColumn) {
		this.sumColumn = sumColumn;
	}

	@Override
	String getMainClause() {
		return SELECT_FROM;
	}

	@Override
	public String getQueryStart() {
		return MessageFormat.format(getMainClause(), sumColumn, tableName);
	}
}
